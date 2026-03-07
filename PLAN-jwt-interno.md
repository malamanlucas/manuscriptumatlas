# Plano: JWT Interno com Sessao de 8 Horas

## Contexto

O Google JWT expira em ~1 hora, forcando re-login frequente. O objetivo e usar o Google JWT apenas para autenticacao inicial e emitir um JWT interno com 8h de validade para todas as chamadas subsequentes.

## Ambientes

| Ambiente | Host | Observacoes |
|----------|------|-------------|
| DEV | `localhost` | `JWT_SECRET` usa default, Google OAuth com `localhost` autorizado |
| PROD | `vocemereceoinferno.com.br` | `JWT_SECRET` via secrets management, cookie `Secure` ativo |

## Decisoes de Design

1. **Substituir `google-jwt` por `internal-jwt`** no `authenticate()` block — o Google JWT sera validado manualmente apenas no endpoint `POST /auth/login`
2. **Sem refresh token** — apos 8h o frontend detecta 401, limpa o token e o usuario re-autentica via Google
3. **JWT signing** via `com.auth0.jwt` (ja incluso transitivamente no `ktor-server-auth-jwt`) com HMAC256
4. **Claims no JWT interno**: `email`, `userId`, `role`, `displayName` — todos via `withClaim("nome", valor)`, nunca `withSubject()`, para manter compatibilidade com `payload.getClaim("email").asString()` existente no `AuthRoutes.kt`
5. **`requireAdmin` otimizado** — fast-fail pelo claim `role` para **rejeitar** nao-admins sem DB lookup, mas **confirmar** admins no DB (protege contra rebaixamento durante a sessao de 8h)
6. **`adminRoutes` dentro do `authenticate("internal-jwt")`** — atualmente estao fora do bloco auth; mover para dentro garante protecao real

## Arquivos a Modificar

### Backend (Kotlin)

#### 1. NOVO: `src/main/kotlin/com/ntcoverage/util/JwtUtil.kt`
- `object JwtUtil` com `init(secret)`, `generateToken(userId, email, role, displayName)`, `getAlgorithm()`, `getIssuer()`
- HMAC256, issuer `"manuscriptum-atlas"`, expiracao 8h
- Claims via `withClaim("email", email)`, `withClaim("userId", userId)`, `withClaim("role", role)`, `withClaim("displayName", displayName)`

#### 2. `src/main/kotlin/com/ntcoverage/model/DTOs.kt`
- Adicionar `LoginRequest(credential: String)` e `LoginResponse(token: String, user: UserDTO)` com `@Serializable`

#### 3. `src/main/kotlin/com/ntcoverage/Application.kt`
- Ler `JWT_SECRET` do env e chamar `JwtUtil.init(secret)` (junto ao `GOOGLE_CLIENT_ID`, ~linha 93)
- Substituir bloco `jwt("google-jwt")` por `jwt("internal-jwt")` que valida com `JwtUtil.getAlgorithm()` / `JwtUtil.getIssuer()`
- Manter `jwkProvider` e `googleClientId` — passa-los para `authLoginRoute()`
- **Antes** do `authenticate("internal-jwt")`, registrar: `authLoginRoute(userRepository, jwkProvider, googleClientId)` (rota publica, sem auth)
- Trocar `authenticate("google-jwt")` por `authenticate("internal-jwt")` (linha 290)
- **Mover `adminRoutes` para dentro do bloco `authenticate("internal-jwt")`** (atualmente estao fora, linhas 279-287)
- Manter logging de autenticacao (`AUTH: login_success`, etc.)

#### 4. `src/main/kotlin/com/ntcoverage/routes/AuthRoutes.kt`
- Nova funcao `fun Route.authLoginRoute(userRepository, jwkProvider, googleClientId)` com `POST /auth/login`:
  - Recebe `LoginRequest` (Google credential)
  - Valida Google JWT manualmente (decode + verify via jwkProvider + checar issuer/email_verified)
  - Busca usuario por email no DB
  - Atualiza lastLogin + picture
  - Loga `AUTH: login_success | email=... | role=...`
  - Gera JWT interno via `JwtUtil.generateToken()`
  - Retorna `LoginResponse(token, user)`
- Atualizar `requireAdmin()`:
  - Se claim `role != ADMIN` -> rejeitar imediatamente (sem DB)
  - Se claim `role == ADMIN` -> confirmar no DB (protege contra rebaixamento mid-session)

#### 5. `docker-compose.yml`
- Adicionar `JWT_SECRET: "${JWT_SECRET:-manuscriptum-dev-secret-change-in-production}"` no service `app`
- DEV (`localhost`): default e suficiente
- PROD (`vocemereceoinferno.com.br`): `JWT_SECRET` deve vir de secrets management, nunca hardcodado

### Frontend (TypeScript)

#### 6. `frontend/types/index.ts`
- Adicionar `LoginResponse { token: string; user: UserDTO }`

#### 7. `frontend/lib/api.ts`
- Adicionar `loginWithGoogle(credential: string): Promise<LoginResponse>`:
  - Usar `fetch()` raw (sem `fetchJsonAuth`, pois ainda nao ha token)
  - Incluir `Content-Type: application/json`
  - POST para `/api/auth/login` com body `{ credential }`

#### 8. `frontend/hooks/useAuth.tsx`
- Alterar `login()`: em vez de armazenar o Google credential e chamar `/auth/me`:
  - Chamar `loginWithGoogle(credential)` do `api.ts`
  - Receber `{ token, user }` direto da resposta
  - Armazenar JWT interno com `setAuthToken(token, 28800)` (8h)
  - Setar `user` diretamente (sem chamar `getAuthMe()` separado)
- `hydrate()` nao muda — ja decodifica `exp` do JWT e chama `/auth/me`

## O que NAO muda

- `AuthGate.tsx`, `Header.tsx` — continuam chamando `login(credential)` normalmente
- `fetchJsonAuth()` — continua lendo token do storage e enviando como Bearer
- `getAuthMe()` — continua funcionando (agora validado pelo JWT interno)
- `UserRepository`, `UserService` — sem alteracao
- Banco de dados — nenhuma nova tabela
- i18n — sem textos novos para o usuario
- Nenhuma dependencia nova no `build.gradle.kts`

## Fluxo Final

```
Google Sign-In -> credential (Google JWT)
  -> POST /auth/login { credential }          [rota publica, sem auth]
    -> Backend valida Google JWT (jwkProvider)
    -> Busca user por email no DB
    -> Atualiza lastLogin + picture
    -> Gera JWT interno (HMAC256, 8h, claims: email/userId/role/displayName)
    -> Retorna { token, user }
  -> Frontend armazena JWT interno (localStorage + cookie, maxAge=28800)
  -> Frontend seta user direto da resposta (sem getAuthMe extra)
  -> Chamadas API usam JWT interno como Bearer
  -> authenticate("internal-jwt") valida com HMAC256
  -> requireAdmin: fast-fail por claim, confirma admin no DB
```

## Ordem de Implementacao

1. `util/JwtUtil.kt` (novo)
2. `model/DTOs.kt` (adicionar DTOs)
3. `routes/AuthRoutes.kt` (adicionar `authLoginRoute`, atualizar `requireAdmin`)
4. `Application.kt` (substituir auth scheme, init JwtUtil, registrar login route, mover adminRoutes para dentro do authenticate)
5. `docker-compose.yml` (env var)
6. `frontend/types/index.ts` (tipo)
7. `frontend/lib/api.ts` (funcao `loginWithGoogle`)
8. `frontend/hooks/useAuth.tsx` (atualizar `login`)

## Riscos e Mitigacoes

| Risco | Mitigacao |
|-------|-----------|
| Usuario rebaixado continua admin por ate 8h | `requireAdmin` confirma role no DB mesmo com claim ADMIN |
| Usuario deletado tem token valido por ate 8h | `getAuthMe` retorna 403 para user nao encontrado; frontend limpa token |
| JWT_SECRET fraco em producao | Default so para dev (localhost); PROD (vocemereceoinferno.com.br) deve usar env var com secret forte |
| Cookie sem flag Secure em dev | `setCookie` ja condiciona `Secure` ao protocolo (`https:`); localhost usa HTTP, prod usa HTTPS |
| Clock skew entre servidor e cliente | `hydrate()` ja checa `exp` do JWT; `acceptLeeway(5)` no verifier |

## Verificacao

### DEV (localhost)
1. `docker compose up --build` — app inicia sem erros
2. Acessar `http://localhost:3000`, clicar "Login com Google" — deve chamar `POST /auth/login` e receber JWT interno
3. Navegar para paginas admin — Bearer token e o JWT interno, nao o Google
4. Esperar token expirar (ou testar com TTL curto) — 401 limpa token e mostra login
5. `GET /auth/me` funciona com o JWT interno
6. Operacoes admin (`/auth/users`) funcionam com role check via JWT + DB
7. Rebaixar um admin no DB — proxima chamada admin deve retornar 403
8. Rotas admin (`/admin/*`) agora exigem autenticacao (antes eram abertas)
9. Cookie **sem** flag `Secure` (HTTP em localhost) — verificar que funciona

### PROD (vocemereceoinferno.com.br)
10. Deploy com `JWT_SECRET` forte via env var (nao o default)
11. Login via Google funciona em `https://vocemereceoinferno.com.br`
12. Cookie **com** flag `Secure` (HTTPS) — verificar que persiste
13. Token JWT interno valido por 8h sem re-login
