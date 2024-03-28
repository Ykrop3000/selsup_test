package com.tests;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
class Description implements Serializable {
    private String participantInn;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Product implements Serializable {
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;
}
@Data
@AllArgsConstructor
@NoArgsConstructor
class Document implements Serializable {
    private Description description;
    private String doc_id;
    private String doc_status;
    private String doc_type;
    private boolean importRequest;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private String production_date;
    private String production_type;
    private List<Product> products;
    private String reg_date;
    private String reg_number;
}


public class CrptApi {
    private final Semaphore semaphore;
    private final String apiUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        Runnable task = () -> {
            while (true) {
                try {
                    Thread.sleep(timeUnit.toMillis(1));
                    semaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(task).start();
    }

    public void createDocument(Document document, String accessToken) throws IOException, InterruptedException {
        semaphore.acquire();

        ObjectMapper mapper = new ObjectMapper();
        String jsonDocument = mapper.writeValueAsString(document);

        URL url = new URL(apiUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + accessToken);
        con.setDoOutput(true);
        try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
            out.write(jsonDocument.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                System.out.println(response.toString());
            }
        } else {
            System.out.println("HTTP POST request failed: " + responseCode);
        }
        con.disconnect();
    }

    public static void main(String[] args) {
        // Пример использования:
        try {
            CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 10);

            Product product = new Product();
            product.setProduction_date("date");
            product.setProducer_inn("inn");
            product.setOwner_inn("inn");
            product.setCertificate_document("document");
            product.setTnved_code("code");
            product.setCertificate_document_date("date");
            product.setCertificate_document_number("number");
            product.setTnved_code("code");
            product.setUit_code("code");
            product.setUitu_code("code");

            Document document = new Document();
            document.setDescription(new Description("description"));
            document.setDoc_id("doc_id");
            document.setDoc_status("doc_status");
            document.setDoc_type("doc_type");
            document.setImportRequest(true);
            document.setParticipant_inn("inn");
            document.setProducer_inn("inn");
            document.setProduction_date("27.03.2024");
            document.setProduction_type("type");
            document.setOwner_inn("inn");
            document.setProducts(List.of(product));

            String accessToken = "your_access_token_here";
            crptApi.createDocument(document, accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}