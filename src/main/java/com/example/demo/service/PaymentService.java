package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PaymentService {

    @Value("${vnpay.tmnCode:TESTCODE}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret:TESTSECRET}")
    private String vnp_HashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_Url;

    @Value("${vnpay.returnUrl:http://localhost:8080/payment/return}")
    private String vnp_ReturnUrl;

    public String buildVNPayUrl(String orderId, long amountVnd, String orderInfo){
        // Demo mode: if using the public tryitnow URL, short-circuit to our own success callback
        if (vnp_Url.contains("/tryitnow/")) {
            return vnp_ReturnUrl + "?vnp_TxnRef=" + urlEncode(orderId) + "&vnp_ResponseCode=00";
        }
        Map<String,String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnp_TmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amountVnd * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_CreateDate", new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i=0;i<fieldNames.size();i++){
            String name = fieldNames.get(i);
            String value = vnpParams.get(name);
            if (value != null && value.length() > 0){
                query.append(name).append("=").append(urlEncode(value));
                hashData.append(name).append("=").append(value);
                if (i < fieldNames.size() - 1) {
                    query.append("&");
                    hashData.append("&");
                }
            }
        }
        String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return vnp_Url + "?" + query;
    }

    private String urlEncode(String s){
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String hmacSHA512(String secret, String data){
        try{
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}


