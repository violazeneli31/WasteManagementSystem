package com.greenbin.wms.system;

import java.util.Random;

/** NFR_14: managers and finance officers confirm their login with a 6-digit code. */
public class TwoFactorAuth {
    private final Random random = new Random();
    private String currentCode;

    /** In a real system this code is sent by SMS. Here the login window shows it. */
    public String sendCode() {
        currentCode = String.format("%06d", random.nextInt(1_000_000));
        return currentCode;
    }

    public boolean verify(String code) {
        boolean ok = currentCode != null && currentCode.equals(code == null ? "" : code.trim());
        if (ok) currentCode = null;          // a code works only once
        return ok;
    }
}
