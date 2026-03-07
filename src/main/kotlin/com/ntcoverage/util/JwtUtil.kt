package com.ntcoverage.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtUtil {
    private const val ISSUER = "manuscriptum-atlas"
    private const val EXPIRATION_MS = 8 * 60 * 60 * 1000L // 8 hours

    private lateinit var algorithm: Algorithm

    fun init(secret: String) {
        algorithm = Algorithm.HMAC256(secret)
    }

    fun generateToken(userId: Int, email: String, role: String, displayName: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withClaim("role", role)
            .withClaim("displayName", displayName)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION_MS))
            .sign(algorithm)
    }

    fun getAlgorithm(): Algorithm = algorithm

    fun getIssuer(): String = ISSUER
}
