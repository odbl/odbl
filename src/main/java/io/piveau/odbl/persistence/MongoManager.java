package io.piveau.odbl.persistence;

import io.piveau.odbl.models.*;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MongoManager {

    Logger LOGGER = LoggerFactory.getLogger(MongoManager.class);

    public MongoClient client;

    public MongoManager(MongoClient client) {
        this.client = client;
    }

    /**
     * Creates the needed collections in Mongo
     * @param result
     */
    public void initDB(Handler<AsyncResult<Void>> result) {
        client.getCollections(collectionsResult -> {
           if(collectionsResult.succeeded()) {
               List<String> collections = collectionsResult.result();

               CompositeFuture.all(Arrays.asList(
                   initCollection(collections, MongoUtils.META_COLLECTION),
                   initCollection(collections, MongoUtils.CHAIN_COLLECTION),
                   initCollection(collections, MongoUtils.DATA_COLLECTION),
                   initCollection(collections, MongoUtils.TRANSACTIONS_COLLECTION),
                   initCollection(collections, MongoUtils.PREPARE_COLLECTION),
                   initCollection(collections, MongoUtils.PREPREPARE_COLLECTION),
                   initCollection(collections, MongoUtils.COMMIT_COLLECTION),
                   initCollection(collections, MongoUtils.VIEWCHANGE_COLLECTION)
               )).onComplete(ar -> {
                   if (ar.succeeded()) {
                       LOGGER.info("Initialized MongoDB");
                       Block genesisBlock = Block.createGenesisBlock();
                       storeBlock(genesisBlock, ar2 -> {
                           if(ar2.succeeded()) {
                               result.handle(Future.succeededFuture());
                               LOGGER.info("Stored Genesis Block");
                           } else {
                               result.handle(Future.succeededFuture());
                               LOGGER.info("Genesis Block already exists.");
                           }
                       });
                   } else {
                       result.handle(Future.failedFuture(ar.cause()));
                   }
               });
           } else {
               result.handle(Future.failedFuture(collectionsResult.cause()));
           }
        });
    }

    public void initAuthorityDB(Handler<AsyncResult<Void>> result) {
        client.getCollections(collectionsResult -> {
            if(collectionsResult.succeeded()) {
                List<String> collections = collectionsResult.result();

                CompositeFuture.all(Arrays.asList(
                    initCollection(collections, MongoUtils.META_COLLECTION),
                    initCollection(collections, MongoUtils.PEER_COLLECTION)
                )).onComplete(ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("Initialized MongoDB");
                        result.handle(Future.succeededFuture());
                    } else {
                        result.handle(Future.failedFuture(ar.cause()));
                    }
                });
            } else {
                result.handle(Future.failedFuture(collectionsResult.cause()));
            }
        });
    }


    private Future<Void> initCollection(List<String> existingCollections, String name) {
        Promise<Void> promise = Promise.promise();
        if(existingCollections.contains(name)) {
            promise.complete();
        } else {
            client.createCollection(name, promise);
        }
        return promise.future();
    }

    public void dropCollections(Handler<AsyncResult<Void>> result) {
        CompositeFuture.all(Arrays.asList(
            //dropCollection(MongoUtils.META_COLLECTION),
            dropCollection(MongoUtils.CHAIN_COLLECTION),
            dropCollection(MongoUtils.DATA_COLLECTION),
            dropCollection(MongoUtils.TRANSACTIONS_COLLECTION),
            dropCollection(MongoUtils.COMMIT_COLLECTION),
            dropCollection(MongoUtils.PREPREPARE_COLLECTION),
            dropCollection(MongoUtils.PREPARE_COLLECTION),
            dropCollection(MongoUtils.VIEWCHANGE_COLLECTION)
        )).onComplete(ar -> {
            if (ar.succeeded()) {
                result.handle(Future.succeededFuture());
                LOGGER.info("Deleted Collections");
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void dropAuthorityCollections(Handler<AsyncResult<Void>> result) {
        CompositeFuture.all(Arrays.asList(
            //dropCollection(MongoUtils.META_COLLECTION),
            dropCollection(MongoUtils.PEER_COLLECTION)
        )).onComplete(ar -> {
            if (ar.succeeded()) {
                result.handle(Future.succeededFuture());
                LOGGER.info("Deleted Collections");
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    private Future<Void> dropCollection(String name) {
        Promise<Void> promise = Promise.promise();
        client.dropCollection(name, promise);
        return promise.future();
    }

    public void upsertPeer(JsonObject object, Handler<AsyncResult<Boolean>> handler) {
        object.put("_id", object.getValue("id"));
        client.save(MongoUtils.PEER_COLLECTION, object, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchPeers(Handler<AsyncResult<List<JsonObject>>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions().setLimit(100).setBatchSize(100);
        client.findWithOptions(MongoUtils.PEER_COLLECTION, query, findOptions, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    public void upsertMetaObject(JsonObject object, Handler<AsyncResult<JsonObject>> handler) {
        object.put("_id", object.getValue("id"));
        client.save(MongoUtils.META_COLLECTION, object, ar -> {
           if(ar.succeeded()) {
               handler.handle(Future.succeededFuture(object));
           } else {
               LOGGER.error(ar.cause().getMessage());
               handler.handle(Future.failedFuture(ar.cause()));
           }
        });
    }

    public void fetchMetaObject(String id, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject query = new JsonObject();
        query.put("_id", id);
        client.findOne(MongoUtils.META_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                handler.handle(Future.succeededFuture(ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    public void storeTransaction(Transaction transaction, Verification verification, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = new JsonObject();
        document.put("_id", transaction.id);
        document.put("transaction", transaction.toJson());
        document.put("verification", verification.toJson());
        client.insert(MongoUtils.TRANSACTIONS_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchTransaction(String id, Handler<AsyncResult<Transaction>> handler) {
        JsonObject query = new JsonObject();
        query.put("_id", id);
        client.findOne(MongoUtils.TRANSACTIONS_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                Transaction transaction = Transaction.fromJson(ar.result().getJsonObject("transaction"));
                handler.handle(Future.succeededFuture(transaction));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void deleteTransaction(String id, Handler<AsyncResult<Void>> handler) {
        JsonObject query = new JsonObject();
        query.put("_id", id);
        client.removeDocument(MongoUtils.TRANSACTIONS_COLLECTION, query, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void storeDataset(DatasetCollection datasetCollection, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = datasetCollection.toJson();
        document.put("_id", datasetCollection.id);
        client.insert(MongoUtils.DATA_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchDataset(String id, Handler<AsyncResult<DatasetCollection>> handler) {
        JsonObject query = new JsonObject();
        query.put("_id", id);
        client.findOne(MongoUtils.DATA_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                DatasetCollection datasetCollection = DatasetCollection.fromJson(ar.result());
                handler.handle(Future.succeededFuture(datasetCollection));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchDatasets(Integer page, Integer limit, Handler<AsyncResult<List<DatasetCollection>>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions().setLimit(limit).setSkip(page*limit);
        client.findWithOptions(MongoUtils.DATA_COLLECTION, query, findOptions, ar -> {
            if(ar.succeeded()) {
                List<DatasetCollection> datasetCollections = new ArrayList<>();
                for(JsonObject result : ar.result()) {
                    datasetCollections.add(DatasetCollection.fromJson(result));
                }
                handler.handle(Future.succeededFuture(datasetCollections));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void countDocuments(String collection,  Handler<AsyncResult<Long>> handler) {
        JsonObject query = new JsonObject();
        client.count(collection, query, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void storePrePrepare(PrePrepare prePrepare, Verification verification, String state, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = new JsonObject();
        document.put("_id", prePrepare.blockHash);
        document.put("blockHash", prePrepare.blockHash);
        document.put("state", state);
        document.put("prePrepare", prePrepare.toJson());
        document.put("verification", verification.toJson());
        client.insert(MongoUtils.PREPREPARE_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchPrePrepare(String blockHash, Handler<AsyncResult<PrePrepare>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        client.findOne(MongoUtils.PREPREPARE_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                PrePrepare prePrepare = PrePrepare.fromJson(ar.result().getJsonObject("prePrepare"));
                handler.handle(Future.succeededFuture(prePrepare));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void getPrePrepareState(String blockHash, Handler<AsyncResult<String>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        client.findOne(MongoUtils.PREPREPARE_COLLECTION, query, new JsonObject().put("state", true), ar -> {
            if(ar.succeeded() && ar.result() != null) {
                handler.handle(Future.succeededFuture(ar.result().getString("state")));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void setPrePrepareState(String blockHash, String state, Handler<AsyncResult<Void>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        JsonObject update = new JsonObject();
        update.put("$set", new JsonObject().put("state", state));
        client.updateCollection(MongoUtils.PREPREPARE_COLLECTION, query, update, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture());
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    public void storePrepare(Prepare prepare, Verification verification, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = new JsonObject();
        document.put("_id",  UUID.randomUUID().toString());
        document.put("blockHash", prepare.blockHash);
        document.put("prepare", prepare.toJson());
        document.put("verification", verification.toJson());
        client.insert(MongoUtils.PREPARE_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchCountPrepareForBlock(String blockHash, Handler<AsyncResult<Long>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        client.count(MongoUtils.PREPARE_COLLECTION, query, ar -> {
           if(ar.succeeded()) {
               handler.handle(Future.succeededFuture(ar.result()));
           } else {
               handler.handle(Future.failedFuture(ar.cause()));
           }
        });
    }

    public void storeCommit(Commit commit, Verification verification, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = new JsonObject();
        document.put("_id",  UUID.randomUUID().toString());
        document.put("blockHash", commit.blockHash);
        document.put("commit", commit.toJson());
        document.put("verification", verification.toJson());
        client.insert(MongoUtils.COMMIT_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.succeededFuture(false));
            }
        });
    }

    public void fetchCountCommitForBlock(String blockHash, Handler<AsyncResult<Long>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        client.count(MongoUtils.COMMIT_COLLECTION, query, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void storeViewChange(ViewChange viewChange, Verification verification, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = new JsonObject();
        document.put("_id",  UUID.randomUUID().toString());
        document.put("blockHash", viewChange.blockHash);
        document.put("viewChange", viewChange.toJson());
        document.put("verification", verification.toJson());
        client.insert(MongoUtils.VIEWCHANGE_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
            } else {
                handler.handle(Future.succeededFuture(false));
            }
        });
    }

    public void fetchCountViewChangeForBlock(String blockHash, Handler<AsyncResult<Long>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", blockHash);
        client.count(MongoUtils.VIEWCHANGE_COLLECTION, query, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(ar.result()));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    public void fetchTransactions(Integer page, Integer limit, Handler<AsyncResult<List<Transaction>>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions().setLimit(limit).setSkip(page*limit);
        client.findWithOptions(MongoUtils.TRANSACTIONS_COLLECTION, query, findOptions, ar -> {
           if(ar.succeeded()) {
               List<Transaction> transactions = new ArrayList<>();
               for(JsonObject result : ar.result()) {
                   transactions.add(Transaction.fromJson(result.getJsonObject("transaction")));
               }
               handler.handle(Future.succeededFuture(transactions));
           }
        });
    }

    public void fetchNextTransaction(Handler<AsyncResult<Transaction>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions().setSort(new JsonObject().put("transaction.timestamp", 1)).setLimit(1);
        client.findWithOptions(MongoUtils.TRANSACTIONS_COLLECTION, query, findOptions, ar -> {
            if(ar.succeeded() && ar.result().size() > 0) {
                JsonObject result = ar.result().get(0).getJsonObject("transaction");
                Transaction transaction = Transaction.fromJson(result);
                handler.handle(Future.succeededFuture(transaction));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }



    public void fetchBlockchain(Integer page, Integer limit, Handler<AsyncResult<List<Block>>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions()
            .setLimit(limit)
            .setSkip(page*limit)
            .setSort(new JsonObject().put("index", 1));
        client.findWithOptions(MongoUtils.CHAIN_COLLECTION, query, findOptions, ar -> {
            if(ar.succeeded()) {
                List<Block> blocks = new ArrayList<>();
                for(JsonObject result : ar.result()) {
                    blocks.add(Block.fromJson(result));
                }
                handler.handle(Future.succeededFuture(blocks));
            }
        });
    }

    public void storeBlock(Block block, Handler<AsyncResult<Boolean>> handler) {
        JsonObject document = block.toJson();
        document.put("_id", String.valueOf(block.index));
        client.insert(MongoUtils.CHAIN_COLLECTION, document, ar -> {
            if(ar.succeeded()) {
                handler.handle(Future.succeededFuture(true));
                // Garbage Collection
                deleteAllByBlock(block, arDeletion -> {

                });
            } else {
                LOGGER.error(ar.cause().getMessage());
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchSingleBlock(Long index, Handler<AsyncResult<Block>> handler) {
        JsonObject query = new JsonObject();
        query.put("_id", String.valueOf(index));
        client.findOne(MongoUtils.CHAIN_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                Block block = Block.fromJson(ar.result());
                handler.handle(Future.succeededFuture(block));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchSingleBlock(String blockHash, Handler<AsyncResult<Block>> handler) {
        JsonObject query = new JsonObject();
        query.put("hash", blockHash);
        client.findOne(MongoUtils.CHAIN_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                Block block = Block.fromJson(ar.result());
                handler.handle(Future.succeededFuture(block));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    public void fetchSingleBlockByTransaction(String transactionId, Handler<AsyncResult<Block>> handler) {
        JsonObject query = new JsonObject();
        query.put("data", transactionId);
        client.findOne(MongoUtils.CHAIN_COLLECTION, query, null, ar -> {
            if(ar.succeeded() && ar.result() != null) {
                Block block = Block.fromJson(ar.result());
                handler.handle(Future.succeededFuture(block));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    public void fetchLastBlock(Handler<AsyncResult<Block>> handler) {
        JsonObject query = new JsonObject();
        FindOptions findOptions = new FindOptions().setSort(new JsonObject().put("index", -1)).setLimit(1);
        client.findWithOptions(MongoUtils.CHAIN_COLLECTION, query, findOptions, ar -> {
            if(ar.succeeded()) {
                JsonObject result = ar.result().get(0);
                Block block = Block.fromJson(result);
                handler.handle(Future.succeededFuture(block));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    public void messageExists(ProtocolMessage message, Handler<AsyncResult<Boolean>> handler) {
        JsonObject query = new JsonObject();
        query.put("verification.issuer", message.getIssuer());
        if(message.isPrePepare()) {
            query.put("prePrepare.transactionId", message.payload.getString("transactionId"));
            client.findOne(MongoUtils.PREPREPARE_COLLECTION, query, null, ar -> {
                if(ar.succeeded() && ar.result() != null) {
                    handler.handle(Future.succeededFuture(true));
                } else {
                    handler.handle(Future.succeededFuture(false));
                }
            });
        } else {
            handler.handle(Future.succeededFuture(false));
        }
    }

    public void deleteMessage(ProtocolMessage message, Handler<AsyncResult<Void>> handler) {
        JsonObject query = new JsonObject();
        query.put("verification.issuer", message.getIssuer());
        if(message.isPrePepare()) {
            query.put("prePrepare.transactionId", message.payload.getString("transactionId"));
            client.findOneAndDelete(MongoUtils.PREPREPARE_COLLECTION, query, ar -> {
                if (ar.succeeded()) {
                    handler.handle(Future.succeededFuture());
                }
            });
        } else {
            handler.handle(Future.succeededFuture());
        }
    }



    public void deleteAllByBlock(Block block, Handler<AsyncResult<Void>> handler) {
        JsonObject query = new JsonObject();
        query.put("blockHash", block.hash);
        client.findOneAndDelete(MongoUtils.PREPREPARE_COLLECTION, query, ar -> {
           if(ar.succeeded() && ar.result() != null) {
               LOGGER.info("Deleted Preprepare");
               JsonObject query2 = new JsonObject();

               query2.put("blockHash", block.hash);
               Promise<MongoClientDeleteResult> preparePromise = Promise.promise();
               client.removeDocuments(MongoUtils.PREPARE_COLLECTION, query2, preparePromise);

               Promise<MongoClientDeleteResult> commitPromise = Promise.promise();
               client.removeDocuments(MongoUtils.COMMIT_COLLECTION, query2, commitPromise);

               Promise<MongoClientDeleteResult> viewChangePromise = Promise.promise();
               client.removeDocuments(MongoUtils.VIEWCHANGE_COLLECTION, query2, viewChangePromise);

               JsonObject query3 = new JsonObject();
               query3.put("_id", block.data);
               Promise<MongoClientDeleteResult> transactionPromise = Promise.promise();
               client.removeDocuments(MongoUtils.TRANSACTIONS_COLLECTION, query3, transactionPromise);

               CompositeFuture.all(Arrays.asList(
                   preparePromise.future(),
                   commitPromise.future(),
                   viewChangePromise.future(),
                   transactionPromise.future()
               )).onComplete(ar2 -> {
                  if(ar2.succeeded()) {
                      handler.handle(Future.succeededFuture());
                  } else {
                      handler.handle(Future.failedFuture(ar2.cause()));
                  }
               });

           } else {
               LOGGER.info("Preprepare already deleted");
               handler.handle(Future.succeededFuture());
           }
        });
    }

}
