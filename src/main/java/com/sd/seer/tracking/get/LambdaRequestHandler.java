package com.sd.seer.tracking.get;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sd.seer.model.Tracking;
import com.sd.seer.model.User;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;

import java.util.HashMap;

public class LambdaRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Input : " + event + "\n");

        String email = event.getPathParameters().get("email");
        if(event.getQueryStringParameters() == null) event.setQueryStringParameters(new HashMap<>());
        Integer size = Integer.valueOf(event.getQueryStringParameters()
                .getOrDefault("size", String.valueOf(Integer.MAX_VALUE)));
        Integer page = Integer.valueOf(event.getQueryStringParameters()
                .getOrDefault("page", String.valueOf(0)));

        logger.log("Requesting to get tracking for user with mail : " + email + "\n");

        // Create a connection to DynamoDB
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDBMapper m = new DynamoDBMapper(client);
        logger.log("Mapper created" + "\n");

        User savedUser = m.load(User.class, email);
        if(savedUser != null) {
            Tracking savedTracking = savedUser.getTracking();
            if(savedTracking == null) {
                savedTracking = new Tracking();
            } else {
                if(savedTracking.getBpms() != null) {
                    savedTracking.getBpms().sort((o1, o2) -> o2.getTime().compareTo(o1.getTime()));
                    if (savedTracking.getBpms().size() > size) {
                        savedTracking.setBpms(savedTracking.getBpms()
                                .subList(page * size, page * size + size));
                    }
                }
                if(savedTracking.getLocations() != null) {
                    savedTracking.getLocations().sort((o1, o2) -> o2.getTime().compareTo(o1.getTime()));
                    if (savedTracking.getLocations().size() > size) {
                        savedTracking.setLocations(savedTracking.getLocations()
                                .subList(page * size, page * size + size));
                    }
                }
            }
            logger.log("Tracking loaded : " + mapper.writeValueAsString(savedTracking) + "\n");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_OK)
                    .withHeaders(new HashMap<String, String>() {
                        {
                            put("Access-Control-Allow-Origin", "*");
                            put("Access-Control-Allow-Headers", "*");
                        }
                    })
                    .withBody(mapper.writeValueAsString(savedTracking));
        } else {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(HttpStatus.SC_NOT_FOUND)
                    .withHeaders(new HashMap<String, String>() {
                        {
                            put("Access-Control-Allow-Origin", "*");
                            put("Access-Control-Allow-Headers", "*");
                        }
                    })
                    .withBody(event.getBody());
        }
    }

}
