package com.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import com.google.gson.Gson;   


import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function 
{

    class SymbolHelper
    {
        public String symbol;
        public String value;
    }

    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("getCacheValue")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) 
    {
        context.getLogger().info("Java HTTP trigger processed a request.");
        // Parse query parameter
        final String query = request.getQueryParameters().get("symbol");

        if (query == null) 
        {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a symbol name on the query string.").build();
        } 
        else 
        {
            boolean useSsl = true;
            String cacheHostname = System.getenv("REDISCACHEHOSTNAME");
            String cachekey      = System.getenv("REDISCACHEKEY");
    
            // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
            Jedis jedis = new Jedis(cacheHostname, 6380, DefaultJedisClientConfig.builder()
                .password(cachekey)
                .ssl(useSsl)
                .build()
            );
    
            String cacheVal = jedis.get(query);

            SymbolHelper sHelper = new SymbolHelper();
            sHelper.symbol = query;
            sHelper.value  = "Not Found";

            if (cacheVal != null && !"".equals(cacheVal))
            {
                sHelper.value  = cacheVal;
            }
    
            jedis.close();
            String resPayload = new Gson().toJson(sHelper);

            return request.createResponseBuilder(HttpStatus.OK).body(resPayload).build();
        }
    }
}
