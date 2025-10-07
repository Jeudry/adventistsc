package com.adventist.adventist.services

import com.adventist.adventist.domain.exceptions.InvalidTokenEx
import com.adventist.adventist.domain.types.UserId
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import kotlin.io.encoding.Base64

@Service
class JwtService(
    @param:Value("\${jwt.secret}") private val secretBase64: String,
    @param:Value("\${jwt.expiration-minutes}") private val expirationMinutes: Int
) {
    private val secretKey = Keys.hmacShaKeyFor(
        Base64.Default.decode(secretBase64)
    )

    private val accessTokenValidityMs: Long = expirationMinutes * 60 * 1000L
 val refreshTokenValidityMs: Long = 30 * 24 * 60 * 60 * 1000L

    private fun generateToken(userId: UserId, type: String, expiry: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiry)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", type)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

     fun generateAccessToken(userId: UserId): String = generateToken(userId, "access", accessTokenValidityMs.toLong())

     fun generateRefreshToken(userId: UserId): String = generateToken(userId, "refresh", refreshTokenValidityMs)

     fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims?.get("type") as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims?.get("type") as? String ?: return false
        return tokenType == "refresh"
    }

    fun getUserIdFromToken(token: String): UserId {
        val claims = parseAllClaims(token) ?: throw InvalidTokenEx(
            "The attached token is invalid."
        )

        return UUID.fromString(claims.subject)
    }

    private fun parseAllClaims(token: String): Claims? {
        val rawToken = if(token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else token

        return try {
            Jwts.parser()
               .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch(e: Exception){
            null
        }
    }
}