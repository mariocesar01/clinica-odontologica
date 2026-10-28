package com.clinica.odontologica.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.*;

@Service
public class MercadoPagoClient {
    private final RestClient client = RestClient.builder().baseUrl("https://api.mercadopago.com").build();
    private final ObjectMapper mapper;
    @Value("${mp.access-token:}") private String token;
    @Value("${mp.public-url:}") private String publicUrl;
    public MercadoPagoClient(ObjectMapper mapper) { this.mapper = mapper; }

    public boolean configurado() { return token != null && !token.isBlank(); }

    public record Preferencia(String id, String url) {}

    public Preferencia criarPreferencia(String referencia, String descricao, BigDecimal valor) {
        if (!configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Configure MP_ACCESS_TOKEN para habilitar pagamentos online.");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", descricao); item.put("quantity", 1);
        item.put("currency_id", "BRL"); item.put("unit_price", valor);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item)); body.put("external_reference", referencia);
        if (publicUrl != null && publicUrl.startsWith("https://")) {
            String host = publicUrl.replaceAll("/+$", "");
            body.put("notification_url", host + "/api/pagamentos/webhook");
            body.put("back_urls", Map.of("success", host + "/home", "failure", host + "/home", "pending", host + "/home"));
        }
        try {
            JsonNode json = client.post().uri("/checkout/preferences")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Idempotency-Key", UUID.nameUUIDFromBytes(referencia.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
            if (json == null || json.path("id").asText().isBlank())
                throw new IllegalStateException("Resposta incompleta do provedor de pagamento.");
            // Checkout Pro clássico: usar a URL devolvida pelo provedor para as credenciais
            // da aplicação. Tokens de teste podem começar com APP_USR, não com TEST-.
            String checkout = json.path("init_point").asText("");
            if (checkout.isBlank()) checkout = json.path("sandbox_init_point").asText("");
            if (checkout.isBlank()) throw new IllegalStateException("Checkout não disponibilizado pelo provedor.");
            return new Preferencia(json.get("id").asText(), checkout);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível criar o checkout no Mercado Pago.");
        }
    }

    /** Lista pagamentos confirmáveis pelo provedor, identificados por referência externa. */
    public List<String> pesquisarIdsPorReferencia(String referencia) {
        if (!configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Pagamento não configurado.");
        if (referencia == null || !referencia.matches("CLINICA-[0-9]+-[a-fA-F0-9-]{36}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referência inválida.");
        try {
            JsonNode json = client.get()
                    .uri(builder -> builder.path("/v1/payments/search")
                            .queryParam("external_reference", referencia).build())
                    .header("Authorization", "Bearer " + token)
                    .retrieve().body(JsonNode.class);
            List<String> ids = new ArrayList<>();
            if (json != null && json.path("results").isArray()) {
                for (JsonNode item : json.path("results")) {
                    String id = item.path("id").asText("");
                    if (id.matches("[0-9]{1,30}") &&
                        referencia.equals(item.path("external_reference").asText(""))) ids.add(id);
                }
            }
            return ids;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível consultar o pagamento no Mercado Pago.");
        }
    }

    public JsonNode consultarPagamento(String id) {
        if (!configurado()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pagamento não configurado.");
        if (id == null || !id.matches("[0-9]{1,30}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de pagamento inválido.");
        try {
            JsonNode json = client.get().uri("/v1/payments/{id}", id)
                    .header("Authorization", "Bearer " + token).retrieve().body(JsonNode.class);
            if (json == null) throw new IllegalStateException("Resposta vazia do provedor.");
            return json;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha na consulta ao provedor.");
        }
    }
}
