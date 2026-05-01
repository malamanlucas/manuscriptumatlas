# Proyecto PagBank — Arquitectura de Seguridad

## Contexto

Trabajo en PagBank, en un proyecto de fuerza de ventas. Mi equipo es responsable de la seguridad de las aplicaciones.

## Lo que construí

Desarrollé toda la arquitectura de autenticación y autorización del proyecto. El núcleo de esto es la emisión de tokens JWT con firma asimétrica — el Authorization Server tiene la clave privada y emite los tokens; los Resource Servers tienen solo la clave pública, pudiendo validar la firma sin nunca tener acceso a la clave privada.

El JWT es autofirmado y lleva los permisos, la información básica del usuario, y viaja entre las aplicaciones de forma segura. Configuré todos los microservicios de recursos para validar el token automáticamente usando Spring Security, lo que facilita mucho ese proceso.

También implementé el mecanismo de introspección de token, que permite revocar credenciales de forma inmediata en caso de cualquier incidente de seguridad.

## SSO con Microsoft

Además, creé un microservicio separado de autenticación integrado con Microsoft — al que llamé "Alfa SSO". Utiliza OAuth 2.0 con Authorization Code Grant, obligando al usuario a autenticarse con su correo corporativo y validar mediante Microsoft Authenticator en su dispositivo Android — MFA.

Después de la autenticación exitosa en el Alfa SSO, la responsabilidad de generar el JWT interno se delega a otro microservicio. Este token interno es especial — diferente de los tokens de los recursos — y es el que circula por las aplicaciones internas.

## Resultado

Con esta arquitectura, garantizo que las credenciales del usuario nunca quedan expuestas, los recursos están protegidos, y ante cualquier problema podemos revocar el acceso de forma inmediata.
