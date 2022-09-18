package com.thordickinson.multiprocess.impl;

import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.thordickinson.multiprocess.api.ProcessingContext;
import com.thordickinson.multiprocess.api.ResultHandler;
import com.thordickinson.multiprocess.api.TaskResult;
import org.bson.Document;

import java.util.ArrayList;

public class MongoWriterHandler<I> implements ResultHandler<I, Any> {

    private final String collectionName;
    private final String databaseName;
    private final String uri;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private int bufferSize = 20;
    private ArrayList<Document> buffer = new ArrayList<>(bufferSize);

    public MongoWriterHandler(String connectionUri, String database, String collectionName) {
        this.collectionName = collectionName;
        this.uri = connectionUri;
        this.databaseName = database;
    }

    @Override
    public void initialize(ProcessingContext ctx) {
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(databaseName);
        collection = database.getCollection(collectionName);
    }

    @Override
    public void terminate() {
        if(mongoClient != null){
            flush();
            mongoClient.close();
            mongoClient = null;
        }
    }

    @Override
    public void handleResult(TaskResult<I, Any> result) {
        String json = JsonStream.serialize(result.result().get());
        Document document = Document.parse(json);
        buffer.add(document);
        if(buffer.size() >= bufferSize ){
            flush();
        }
    }

    private void flush(){
        if(buffer.isEmpty()) return;
        collection.insertMany(buffer);
        buffer.clear();
    }
}
