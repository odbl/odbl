package io.piveau.odbl.services.node;

import io.piveau.odbl.Constants;
import io.piveau.odbl.Utils;
import io.piveau.odbl.handler.NodeHandler;
import io.piveau.odbl.models.*;
import io.piveau.odbl.persistence.MongoManager;
import io.piveau.odbl.persistence.MongoUtils;
import io.piveau.odbl.services.protocol.ProtocolMessageFactory;
import io.piveau.odbl.services.protocol.ProtocolUtils;
import io.piveau.odbl.utils.CryptoManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NodeServiceImpl implements NodeService {

    Logger LOGGER = LoggerFactory.getLogger(NodeServiceImpl.class);

    private final MongoManager mng;
    private Vertx vertx;
    private JsonObject config;
    private ProtocolMessageFactory protocolMessageFactory;

    public NodeServiceImpl(Vertx vertx, JsonObject config, MongoManager mongoManager, Handler<AsyncResult<NodeService>> readyHandler) {
        this.mng = mongoManager;
        this.vertx = vertx;
        this.config = config;
        this.protocolMessageFactory = new ProtocolMessageFactory(vertx);
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public void postTransactionDebug(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {
        if(payload.containsKey("bulk")) {
            JsonArray datasets = payload.getJsonArray("payload");
            List<Transaction> transactions = new ArrayList<>();
            datasets.forEach(dataset -> {
                if(dataset instanceof JsonObject) {
                    Transaction transaction = new Transaction(
                        UUID.randomUUID().toString(),
                        ((JsonObject) dataset).encode(),
                        "JSON-LD",
                        Utils.getCurrentTimestamp()
                    );
                    transactions.add(transaction);
                }
            });

            postSingleTransaction(0, transactions, ar -> {
                if(ar.succeeded()) {
                    result.handle(Future.succeededFuture(new JsonObject().put("message", "success")));
                } else {
                    result.handle(Future.succeededFuture(new JsonObject().put("message", "Failed")));
                }
            });
        } else {
            Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                payload.encode(),
                "JSON-LD",
                Utils.getCurrentTimestamp()
            );
            vertx.eventBus().request(ProtocolUtils.ADDR_DEBUG, transaction.toJson(), ar -> {
                if(ar.succeeded()) {
                    result.handle(Future.succeededFuture(new JsonObject().put("message", ar.result().body().toString())));
                } else {
                    result.handle(Future.succeededFuture(new JsonObject().put("message", "Failed")));
                }
            });
        }

    }

    private void postSingleTransaction(int index,  List<Transaction> transactions, Handler<AsyncResult<Void>> result) {
        vertx.eventBus().request(ProtocolUtils.ADDR_DEBUG, transactions.get(index).toJson(), ar -> {
            if(ar.succeeded()) {
                if(index < transactions.size() - 1) {
                    postSingleTransaction(index + 1, transactions, result);
                } else {
                    result.handle(Future.succeededFuture());
                }
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }


    @Override
    public void postTransaction(JsonObject payload, Handler<AsyncResult<JsonObject>> result) {
        // ToDo Preprocess the DCAT data - check format
        Transaction transaction = new Transaction(
            UUID.randomUUID().toString(),
            payload.encode(),
            "JSON-LD",
            Utils.getCurrentTimestamp()
        );
        ProtocolMessage message = protocolMessageFactory.createTransactionMessage(transaction);
        LOGGER.debug("Created transaction {}", transaction.id);
        vertx.eventBus().send(ProtocolUtils.ADDR_BROADCAST, message.toJson());
        result.handle(Future.succeededFuture(new JsonObject().put("message", "Received transaction")));
    }

    @Override
    public void getTransactions(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result) {
        JsonObject response = new JsonObject();
        response.put("success", true);
        mng.countDocuments(MongoUtils.TRANSACTIONS_COLLECTION, ar -> {
            if(ar.succeeded()) {
                response.put("count", ar.result());
                mng.fetchTransactions(page, limit, ar2 -> {
                    if(ar2.succeeded()) {
                        JsonArray results = new JsonArray();
                        for(Transaction transaction : ar2.result()) {
                            results.add(transaction.toJson());
                        }
                        response.put("results", results);
                        result.handle(Future.succeededFuture(response));
                    } else {
                        result.handle(Future.failedFuture(ar2.cause()));
                    }
                });
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void getBlockchain(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result) {
        JsonObject response = new JsonObject();
        response.put("success", true);
        mng.countDocuments(MongoUtils.CHAIN_COLLECTION, ar -> {
            if(ar.succeeded()) {
                response.put("count", ar.result());
                mng.fetchBlockchain(page, limit, ar2 -> {
                    if(ar2.succeeded()) {
                        JsonArray results = new JsonArray();
                        for(Block block : ar2.result()) {
                            results.add(block.toJson());
                        }
                        response.put("results", results);
                        result.handle(Future.succeededFuture(response));
                    } else {
                        result.handle(Future.failedFuture(ar2.cause()));
                    }
                });
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    @Override
    public void getDatasets(Integer page, Integer limit, Handler<AsyncResult<JsonObject>> result) {
        JsonObject response = new JsonObject();
        response.put("success", true);
        mng.countDocuments(MongoUtils.DATA_COLLECTION, ar -> {
            if(ar.succeeded()) {
                response.put("count", ar.result());
                mng.fetchDatasets(page, limit, ar2 -> {
                    if(ar2.succeeded()) {
                        JsonArray results = new JsonArray();
                        for(DatasetCollection datasetCollection : ar2.result()) {
                            results.add(getDatasetForView(datasetCollection));
                        }
                        response.put("results", results);
                        result.handle(Future.succeededFuture(response));
                    } else {
                        result.handle(Future.failedFuture(ar2.cause()));
                    }
                });
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private JsonObject getDatasetForView(DatasetCollection datasetCollection) {
        JsonObject result = new JsonObject();
        JsonArray datasets = datasetCollection.datasets;
        Dataset dataset = Dataset.fromJson(datasets.getJsonObject(0));

        result.put("id", datasetCollection.id);
        result.put("issuer", datasetCollection.issuer);
        result.put("blockHash", dataset.blockHash);
        result.put("issued", datasetCollection.issued);
        result.put("updated", datasetCollection.updated);
        try {
            result.put("payload", new JsonObject(dataset.payload));
        } catch (Exception e) {
            result.put("payload", new JsonObject()
                .put("note", "Could not be parsed")
                .put("rawData", dataset.payload)
            );
        }
        return result;
    }

    @Override
    public void getDataset(String id, Handler<AsyncResult<JsonObject>> result) {
        mng.fetchDataset(id, ar -> {
            if(ar.succeeded()) {
                DatasetCollection datasetCollection = ar.result();
                JsonObject response = getDatasetForView(datasetCollection);
                mng.fetchSingleBlock(response.getString("blockHash"), ar2 -> {
                    if(ar2.succeeded()) {
                        response.put("block", ar2.result().toJson());
                        result.handle(Future.succeededFuture(response));
                    } else {
                        result.handle(Future.failedFuture(ar2.cause()));
                    }
                });
            } else {
                result.handle(Future.failedFuture(ar.cause()));
            }
        });

    }
}
