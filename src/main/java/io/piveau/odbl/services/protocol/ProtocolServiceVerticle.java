package io.piveau.odbl.services.protocol;

import io.piveau.odbl.Constants;
import io.piveau.odbl.Utils;
import io.piveau.odbl.models.*;
import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.utils.NetworkEnvironment;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ProtocolServiceVerticle extends AbstractVerticle {

    Logger LOGGER = LoggerFactory.getLogger(ProtocolServiceVerticle.class);

    private JsonObject config;
    private WebClient webClient;
    private HttpClient httpClient;
    private ProtocolUtils protocolUtils;
    private ProtocolMessageFactory protocolMessageFactory;
    private MongoManager mongoManager;
    private NetworkEnvironment networkEnvironment;
    private boolean transLock;

    public ProtocolServiceVerticle(MongoManager mongoManager, JsonObject config, Vertx vertx) {
        this.config = config;
        this.mongoManager = mongoManager;
        this.protocolMessageFactory = new ProtocolMessageFactory(vertx);
        this.transLock = false;
        this.networkEnvironment = new NetworkEnvironment(mongoManager, vertx, config);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        vertx.eventBus().consumer(ProtocolUtils.ADDR_ACTION, this::action);
        vertx.eventBus().consumer(ProtocolUtils.ADDR_BROADCAST, this::broadcast);
        vertx.eventBus().consumer(ProtocolUtils.ADDR_SYNC, this::sync);
        vertx.eventBus().consumer(ProtocolUtils.ADDR_BLOCK, this::getBlock);
        vertx.eventBus().consumer(ProtocolUtils.ADDR_DEBUG, this::debugAdd);

        HttpClientOptions httpClientOptions = new HttpClientOptions();
        // ToDo Check this
        httpClientOptions.setMaxWebSocketFrameSize(1000000000).setMaxWebSocketMessageSize(1000000000);
        this.httpClient = vertx.createHttpClient(httpClientOptions);

        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setKeepAlive(false);
        this.webClient = WebClient.create(vertx, webClientOptions);

        protocolUtils = new ProtocolUtils(webClient, vertx);
        startPromise.complete();
        LOGGER.info("Deployed ProtocolServiceVerticle");

        vertx.setPeriodic(config.getLong(Constants.TACT, 2000L), handler -> {
            processTransaction();
        });
        vertx.setPeriodic(120000, handler -> {
           mongoManager.fetchLastBlock(ar -> {
               if(ar.succeeded()) {
                   Block lastBlock = ar.result();
                   long diff = Utils.getCurrentTimestamp() - lastBlock.timestamp;
                   if(diff > 60) {
                       LOGGER.info("Elect new primary");
                       //transLock = false;
                       ViewChange viewChange = new ViewChange(
                           ar.result().hash,
                           Utils.getCurrentTimestamp(),
                           networkEnvironment.getNextPrimary(),
                           ar.result().data
                       );
                       ProtocolMessage protocolMessage = protocolMessageFactory.createViewChangeMessage(viewChange);
                       broadcastToAllNodes(protocolMessage);
                   }
               }
           });
        });
        if(config.getBoolean(Constants.AUTO_SYNC)) {
            vertx.eventBus().send(ProtocolUtils.ADDR_SYNC, new JsonObject());
        }
        vertx.setPeriodic(10000, handler -> {
            vertx.eventBus().send(ProtocolUtils.ADDR_SYNC, new JsonObject());
        });

    }

    private void action(Message<JsonObject> message) {
        ProtocolMessage prtclMessage = ProtocolMessage.fromJson(message.body());

        if(prtclMessage.isAuthority()) {
            LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
            actionAuthority(prtclMessage);
        } else {
            if(protocolUtils.verifyProtocolMessage(prtclMessage)) {
                if(prtclMessage.isTransaction()) {
                    LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
                    actionTransaction(prtclMessage);
                } else if(prtclMessage.isPrePepare()) {
                    LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
                    actionPrePrepare(prtclMessage);
                } else if(prtclMessage.isPrepare()) {
                    LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
                    actionPrepare(prtclMessage);
                } else if(prtclMessage.isCommit()) {
                    LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
                    actionCommit(prtclMessage);
                } else if(prtclMessage.isViewChange()) {
                    LOGGER.debug("Received {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
                    actionViewChange(prtclMessage);
                } else {
                    LOGGER.error("Unknown Message Type");
                }
            } else {
                LOGGER.debug("Could not verify message {} from {}", prtclMessage.getIdentity(), prtclMessage.getIssuer());
            }
        }
    }

    private void actionAuthority(ProtocolMessage message) {
        JsonObject payload = message.payload;
        if(payload.getString("action").equals("peersUpdated")) {
            networkEnvironment.updatePeers(ar -> {
                LOGGER.debug("Updated Peers");
            });
        }
    }


    private void actionTransaction(ProtocolMessage message) {
        Transaction transaction = Transaction.fromJson(message.payload);
        Verification verification = Verification.fromJson(message.verification);

        mongoManager.storeTransaction(transaction, verification, ar -> {
            if(ar.succeeded()) {
                // ToDo Do this right
                // ToDo Set timer to check if primary fails
               processTransaction();
            }
        });
    }

    private void processTransaction() {
        if(NetworkEnvironment.isCurrentNodePrimary(vertx)) {
            //transLock = true;
            mongoManager.fetchNextTransaction(trans -> {
                if(trans.succeeded()) {
                    LOGGER.debug("I am primary for transaction {}", trans.result().id);
                    sendPrePrepareMessage(trans.result(), ar2 -> {
                        if(ar2.failed()){
                            LOGGER.error("Sending Pre-Prepare with Transaction {} failed because {}", trans.result().id, ar2.cause());
                        }
                    });
                } else {
                    //transLock = false;
                }
            });
        }
    }

    private void actionPrePrepare(ProtocolMessage message) {
        PrePrepare prePrepare = PrePrepare.fromJson(message.payload);
        Verification verification = Verification.fromJson(message.verification);

        mongoManager.messageExists(message, arMessage -> {
            if(arMessage.succeeded() && !arMessage.result()) {
                LOGGER.debug("Transaction {} was not processed yet", prePrepare.transactionId);

                mongoManager.fetchSingleBlockByTransaction(prePrepare.transactionId, arBlock -> {
                    if(arBlock.failed()) {

                        mongoManager.fetchLastBlock(arLatestBlock -> {
                            if(arLatestBlock.succeeded()) {
                                Block latestBlock = arLatestBlock.result();
                                if(latestBlock.index + 1 == prePrepare.block.getLong("index")) {
                                    mongoManager.storePrePrepare(prePrepare, verification, PrePrepare.STATE_PREPARED, ar -> {
                                        if(ar.succeeded()) {
                                            LOGGER.debug("Stored {} ", message.getIdentity());
                                            Prepare prepare = new Prepare(prePrepare.blockHash, Utils.getCurrentTimestamp(), prePrepare.transactionId);
                                            ProtocolMessage prepareMessage = protocolMessageFactory.createPrepareMessage(prepare);
                                            broadcastToAllNodes(prepareMessage);
                                        } else {
                                            LOGGER.error(ar.cause().getMessage());
                                        }
                                    });
                                }
                            }
                        });

                    } else {
                        mongoManager.deleteTransaction(prePrepare.transactionId, arGarbage -> {
                            if(arGarbage.succeeded()) {
                                LOGGER.debug("Deleted Transaction");
                            } else {
                                LOGGER.debug(arGarbage.cause().getMessage());
                            }

                        });
                    }
                });
            } else {
                LOGGER.debug("Transaction {} was ALREADY processed", prePrepare.transactionId);
            }
        });

    }

    private void sendPrePrepareMessage(Transaction transaction, Handler<AsyncResult<Void>> handler) {
        mongoManager.fetchLastBlock(ar -> {
            if(ar.succeeded()) {
                // Create the next new block
                Block newBlock = protocolUtils.createNewBlock(
                    transaction,
                    ar.result(),
                    NetworkEnvironment.getNodeID(vertx)
                );
                LOGGER.debug("Proposal for a new Block: {}", newBlock.toString());

                PrePrepare prePrepare = new PrePrepare(
                    PrePrepare.STATE_PREPREPARED,
                    Utils.getCurrentTimestamp(),
                    newBlock.hash,
                    newBlock.toJson(),
                    transaction.id
                );

                ProtocolMessage prePrepareMessage = protocolMessageFactory.createPrePrepareMessage(prePrepare);
                mongoManager.messageExists(prePrepareMessage, ar2 -> {
                    if(ar2.succeeded() && !ar2.result()) {
                        broadcastToAllNodes(prePrepareMessage);
                    } else {
                        mongoManager.deleteMessage(prePrepareMessage, arDeletion -> {} );
                    }
                });
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void actionPrepare(ProtocolMessage message) {
        Prepare prepare = Prepare.fromJson(message.payload);
        Verification verification = Verification.fromJson(message.verification);
        // ToDo Do the actual validation
        mongoManager.storePrepare(prepare, verification, ar -> {
            if(ar.succeeded()) {
                mongoManager.fetchCountPrepareForBlock(prepare.blockHash, ar2 -> {
                    Long count = ar2.result();
                    if(count >= protocolUtils.getMinApproval(config)) {
                        LOGGER.debug("Received enough prepare for blockHash {}", prepare.blockHash);
                        sendCommitMessage(prepare);
                    }
                });
            }
        });
    }

    private void sendCommitMessage(Prepare prepare) {
        Commit commit = new Commit(
            prepare.blockHash,
            Utils.getCurrentTimestamp(),
            prepare.transactionId
        );
        ProtocolMessage commitMessage = protocolMessageFactory.createCommitMessage(commit);
        Verification verification = Verification.fromJson(commitMessage.verification);
        mongoManager.getPrePrepareState(prepare.blockHash, ar -> {
            if(ar.succeeded()) {
                if(ar.result().equals(PrePrepare.STATE_COMMITED)) {
                    LOGGER.debug("Commit for blockHash {} was already sent", prepare.blockHash);
                } else {
                    mongoManager.setPrePrepareState(prepare.blockHash, PrePrepare.STATE_COMMITED, ar2 -> {
                        if(ar2.succeeded()) {
                            mongoManager.storeCommit(commit, verification, ar3 -> {
                                if(ar3.succeeded()) {
                                    if(ar3.result()) {
                                        broadcastToAllNodes(commitMessage);
                                    } else {
                                        LOGGER.debug("Commit for blockHash {} was already sent", prepare.blockHash);
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void actionCommit(ProtocolMessage message) {
        Commit commit = Commit.fromJson(message.payload);
        Verification verification = Verification.fromJson(message.verification);
        mongoManager.storeCommit(commit, verification, ar -> {
            if(ar.succeeded()) {
                mongoManager.fetchCountCommitForBlock(commit.blockHash, ar2 -> {
                    Long count = ar2.result();
                    if(count >= protocolUtils.getMinApproval(config)) {
                        LOGGER.debug("Received enough commits for blockHash {}", commit.blockHash);
                        addBlock(commit.blockHash);
                    }
                });
            }
        });
    }

    private void addBlock(String blockHash) {
        mongoManager.getPrePrepareState(blockHash, ar -> {
            if(ar.succeeded()) {
                if(ar.result().equals(PrePrepare.STATE_FINALCOMMITTED)) {
                    LOGGER.debug("Block {} was already added", blockHash);
                } else {
                    mongoManager.setPrePrepareState(blockHash, PrePrepare.STATE_FINALCOMMITTED, ar2 -> {
                        if(ar2.succeeded()) {
                            mongoManager.fetchPrePrepare(blockHash, ar3 -> {
                                if(ar3.succeeded()) {
                                    PrePrepare prePrepare = ar3.result();
                                    Block block = Block.fromJson(prePrepare.block);
                                    // ToDo Store entire conensus chain
                                    mongoManager.storeBlock(block, ar4 -> {
                                        if(ar4.succeeded()) {
                                            LOGGER.info("Added new block {}", block.hash);
                                            storeData(block, ar5 -> {
                                                sendViewChange(block);
                                            });
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    private void storeData(Block block, Handler<AsyncResult<Void>> handler) {
        mongoManager.fetchTransaction(block.data, ar -> {
            if(ar.succeeded()) {
                Transaction transaction = ar.result();

                // ToDo Issue is not correct
                Dataset dataset =  new Dataset(
                    block.issuer,
                    transaction.payload,
                    block.hash,
                    "active",
                    Instant.now()
                );

                DatasetCollection datasetCollection = new DatasetCollection(
                    transaction.id,
                    new JsonArray().add(dataset.toJson()),
                    block.issuer,
                    Instant.now(),
                    Instant.now(),
                    "new"
                );

                mongoManager.storeDataset(datasetCollection, ar2 -> {
                    LOGGER.debug("Stored Dataset {}", datasetCollection.id);
                    handler.handle(Future.succeededFuture());
                });
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void sendViewChange(Block block) {
        JsonArray peers = NetworkEnvironment.getPeers(vertx).getJsonArray("validPeers");
        int noPeers = peers.size();
        int nextPrimary = Math.floorMod(block.hash.hashCode(), noPeers);

        ViewChange viewChange = new ViewChange(
            block.hash,
            Utils.getCurrentTimestamp(),
            Long.toString(nextPrimary),
            block.data
        );
        LOGGER.debug("Proposal for next primary: {}", nextPrimary);
        ProtocolMessage protocolMessage = protocolMessageFactory.createViewChangeMessage(viewChange);
        broadcastToAllNodes(protocolMessage);
    }

    private void actionViewChange(ProtocolMessage message) {
        ViewChange viewChange = ViewChange.fromJson(message.payload);
        Verification verification = Verification.fromJson(message.verification);
        // ToDo Validate View-Change
        mongoManager.storeViewChange(viewChange, verification, ar -> {
            if(ar.succeeded()) {
                mongoManager.fetchCountViewChangeForBlock(viewChange.blockHash, ar2 -> {
                    if(ar2.succeeded()) {
                        Long count = ar2.result();
                        if(count >= protocolUtils.getMinApproval(config)) {
                            LOGGER.debug("Received enough view-changes for blockHash {}", viewChange.blockHash);
                            executeViewChange(viewChange);
                        }
                    }
                });
            }
        });
    }

    private void executeViewChange(ViewChange viewChange) {
        // ToDo Set new Primary
        mongoManager.fetchSingleBlock(viewChange.blockHash, ar -> {
            if(ar.succeeded()) {
                mongoManager.deleteAllByBlock(ar.result(), ar2 -> {
                    if(ar2.succeeded()) {
                        LOGGER.debug("Deleted all deprecated Logs");
                        networkEnvironment.setNewPrimary(viewChange.nextPrimary, ar3 -> {
                            if(ar3.succeeded()) {
                                LOGGER.debug("Set new primary to {}", ar3.result());
                                //transLock = false;
                            } else {
                                LOGGER.error(ar3.cause().getMessage());
                            }
                        });
                    }
                });
            }
        });

    }

    private void broadcastToAllNodes(ProtocolMessage protocolMessage) {
        vertx.eventBus().send(ProtocolUtils.ADDR_BROADCAST, protocolMessage.toJson());
    }

    private void broadcast(Message<JsonObject> message) {
        ProtocolMessage prtclMessage = ProtocolMessage.fromJson(message.body());
        LOGGER.debug("Broadcast {} from {} ", prtclMessage.getIdentity(), NetworkEnvironment.getNodeID(vertx));
        JsonArray nodes = NetworkEnvironment.getPeers(vertx).getJsonArray("validPeers");
        nodes.forEach(node -> {
            if(node instanceof JsonObject) {
                protocolUtils.sendToNode(((JsonObject) node).getString("url"), prtclMessage.toJson());
            }
        });
    }

    private void sync(Message<JsonObject> message) {
        LOGGER.debug("Received Synchronization Action");
        // ToDo Try more than one peer
        protocolUtils.findRandomAvailablePeer(ar -> {
            if(ar.succeeded()) {
                mongoManager.fetchLastBlock(ar2 -> {
                    if(ar.succeeded()) {
                        Block lastBlock = ar2.result();
                        executeSync(lastBlock.index, ar.result());
                    }
                });
            } else {
                LOGGER.debug("Couldn't find any available peer. :(");
            }
        });
    }

    private void executeSync(long lastBlock, JsonObject peer) {
        String baseURL = protocolUtils.getWebSocketURL(peer.getString("url"));
        String url = baseURL + "/protocol/sync";
        LOGGER.info("Execute Sync with {}", url);
        httpClient.webSocketAbs(url, null, null, null, res -> {
            if(res.succeeded()) {
                LOGGER.info("Connected to {}", url);
                WebSocket socket = res.result();
                JsonObject request = new JsonObject();
                AtomicLong blockID = new AtomicLong(lastBlock+1);
                request.put("blockID", blockID.getAndIncrement());
                socket.writeTextMessage(request.encode());
                socket.handler(ar -> {
                    try {
                        String data = new String(ar.getBytes());
                        JsonObject response = new JsonObject(data);
                        if (response.getString("status").equals("ok")) {
                            storeSyncPayload(response, ar2 -> {
                                if(ar2.succeeded()) {
                                    LOGGER.debug("Added block {}", ar2.result().hash);
                                    request.put("blockID", blockID.getAndIncrement());
                                    socket.writeTextMessage(request.encode());
                                } else {
                                    request.put("blockID", blockID.getAndIncrement());
                                    socket.writeTextMessage(request.encode());
                                    LOGGER.error("Sync failed {}", ar2.cause().getMessage());
                                }
                            });
                        } else {
                            socket.close(ar2 -> {
                                if (ar2.succeeded()) {
                                    LOGGER.info("Closed Connection");
                                }
                            });
                        }
                    } catch (Exception e) {
                        socket.close();
                        LOGGER.error(e.getMessage());
                    }
                });
            } else {
                LOGGER.error(res.cause().getMessage());
            }
        });
    }

    private void storeSyncPayload(JsonObject payload, Handler<AsyncResult<Block>> handler) {
        Block block = Block.fromJson(payload.getJsonObject("block"));
        DatasetCollection datasetCollection = DatasetCollection.fromJson(payload.getJsonObject("dataset"));
        mongoManager.storeBlock(block, ar -> {
            if(ar.succeeded()) {
                mongoManager.storeDataset(datasetCollection, ar2 -> {
                    if(ar2.succeeded()) {
                        handler.handle(Future.succeededFuture(block));
                    } else {
                        handler.handle(Future.failedFuture(ar.cause()));
                    }
                });
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private void getBlock(Message<JsonObject> message) {
        Long blockID = message.body().getLong("blockID");
        mongoManager.fetchSingleBlock(blockID, ar -> {
            JsonObject response = new JsonObject();
            if(ar.succeeded()) {
                Block block = ar.result();
                mongoManager.fetchDataset(block.data, ar2 -> {
                    if(ar2.succeeded()) {
                        response.put("status", "ok");
                        response.put("block", block.toJson());
                        response.put("dataset", ar2.result().toJson());
                        //LOGGER.debug(response.encodePrettily());
                        message.reply(response);
                    } else {
                        LOGGER.debug(ar2.cause().getMessage());
                        response.put("status", "notfound");
                        message.reply(response);
                    }
                });
            } else {
                response.put("status", "notfound");
                message.reply(response);
            }
        });
    }

    private void debugAdd(Message<JsonObject> message) {
        Transaction transaction = Transaction.fromJson(message.body());
        mongoManager.fetchLastBlock(ar -> {
            if(ar.succeeded()) {
                Block newBlock = protocolUtils.createNewBlock(
                    transaction,
                    ar.result(),
                    NetworkEnvironment.getNodeID(vertx)
                );

                Dataset dataset =  new Dataset(
                    newBlock.issuer,
                    transaction.payload,
                    newBlock.hash,
                    "active",
                    Instant.now()
                );

                DatasetCollection datasetCollection = new DatasetCollection(
                    transaction.id,
                    new JsonArray().add(dataset.toJson()),
                    newBlock.issuer,
                    Instant.now(),
                    Instant.now(),
                    "new"
                );

                mongoManager.storeBlock(newBlock, ar2 -> {
                    if(ar2.succeeded()) {
                        mongoManager.storeDataset(datasetCollection, ar3 -> {
                            if(ar3.succeeded()) {
                                LOGGER.debug("Added Block/Dataset, Block-Index: {}", newBlock.index);
                                message.reply("Added Block/Dataset, BlockHash: " + newBlock.hash);
                            } else {
                                message.fail(400, ar3.cause().getMessage());
                            }
                        });

                    } else {
                        message.fail(400, ar2.cause().getMessage());
                    }
                });

            } else {
                message.fail(400, ar.cause().getMessage());
            }
        });



    }


}
