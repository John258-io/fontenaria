package mz.edu.fontanaria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mz.edu.fontanaria.models.UsuarioSessao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Emite e valida os tokens JWT da API.
 *
 * O token carrega o id, o nome e o perfil do utilizador; a validade é curta
 * (app.jwt.expiracao-minutos) e o cliente envia-o em cada pedido no header
 * "Authorization: Bearer <token>".
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.expiracao-minutos:480}") long expiracaoMinutos) {
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalStateException("app.jwt.secret deve ter pelo menos 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /** Cria o token de acesso do utilizador. */
    public String gerar(UsuarioSessao usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(expiracaoMinutos * 60)))
                .signWith(chave)
                .compact();
    }

    /** Devolve o utilizador do token, ou vazio se o token for inválido ou expirado. */
    public Optional<UsuarioSessao> validar(String token) {
        try {
            Claims cargas = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new UsuarioSessao(
                    Long.valueOf(cargas.getSubject()),
                    cargas.get("nome", String.class),
                    cargas.get("perfil", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
