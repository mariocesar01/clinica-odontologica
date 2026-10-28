package com.clinica.odontologica.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.HexFormat;

@Service
public class AssinaturaWebhookService {
    @Value("${mp.webhook-secret:}") private String segredo;

    public boolean assinaturaValida(String assinatura, String requestId, String dataId) {
        if (segredo == null || segredo.isBlank() || assinatura == null || requestId == null || dataId == null)
            return false;
        Map<String, String> partes = new HashMap<>();
        for (String trecho : assinatura.split(",")) {
            String[] par = trecho.trim().split("=", 2);
            if (par.length == 2) partes.put(par[0], par[1]);
        }
        String ts = partes.get("ts"); String v1 = partes.get("v1");
        if (ts == null || v1 == null || !v1.matches("[0-9a-fA-F]{64}")) return false;
        try {
            long segundos = Long.parseLong(ts);
            // O cabeçalho é emitido em segundos. Rejeita notificações muito antigas e datas futuras.
            if (Math.abs(Instant.now().getEpochSecond() - segundos) > 300) return false;
            String manifesto = "id:" + dataId.toLowerCase(Locale.ROOT) + ";request-id:" + requestId + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] esperado = mac.doFinal(manifesto.getBytes(StandardCharsets.UTF_8));
            byte[] recebido = HexFormat.of().parseHex(v1);
            return MessageDigest.isEqual(esperado, recebido);
        } catch (Exception e) { return false; }
    }
}
