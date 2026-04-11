package com.ntcoverage.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtUtil {
    private const val ISSUER = "manuscriptum-atlas"
    private const val EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

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

    fun generateServiceToken(clientId: String): String {
        return JWT.create()
            .withIssuer(ISSUER)
            .withClaim("userId", -1)
            .withClaim("email", "$clientId@service.manuscriptum.local")
            .withClaim("role", "ADMIN")
            .withClaim("displayName", "Service: $clientId")
            .withClaim("serviceAccount", true)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000)) // 24h
            .sign(algorithm)
    }

    fun getAlgorithm(): Algorithm = algorithm

    fun getIssuer(): String = ISSUER
}
