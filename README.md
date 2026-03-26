# SecureNube

Aplicación web segura construida como parte del **Taller de Arquitectura Segura** (TDSE).  
Consiste en un cliente estático servido por Apache y una API REST en Spring Boot, ambos desplegados en AWS EC2 con TLS mediante Let's Encrypt.

---

## Arquitectura

```
┌──────────────────────────┐           HTTPS            ┌──────────────────────────┐
│     EC2 - Apache         │ ─────────────────────────► │     EC2 - Spring Boot    │
│  ignaciocastillo         │                             │  andresrendon            │
│  .duckdns.org            │        puerto 8443          │  .duckdns.org            │
│                          │                             │                          │
│  index.html              │                             │  /api/auth/register      │
│  app.js                  │                             │  /api/auth/login         │
│  styles.css              │                             │  /api/hello              │
│                          │                             │                          │
│  Puerto 443 (HTTPS)      │                             │  BCrypt · JPA · H2       │
│  Puerto 80  (→ HTTPS)    │                             │  Puerto 8443 (HTTPS)     │
└──────────────────────────┘                             └──────────────────────────┘
         ▲                                                           ▲
         │ Let's Encrypt                                             │ Let's Encrypt
         │ TLS cert                                                  │ PKCS12 keystore
```

---

## Estructura del repositorio

```
SecureNube/
├── client/                          # Cliente estático servido por Apache
│   ├── index.html                   # Interfaz con formularios de registro/login/hello
│   ├── app.js                       # Lógica async de llamadas a la API
│   └── styles.css                   # Estilos (tema oscuro)
├── secure/                          # API REST Spring Boot
│   ├── pom.xml                      # Dependencias Maven (Spring Boot 3.2.4)
│   └── src/main/java/com/tdse/secure/
│       ├── Application.java         # Punto de entrada
│       ├── config/
│       │   ├── SecurityConfig.java  # HTTP Basic, BCrypt, rutas protegidas
│       │   └── CorsConfig.java      # CORS para permitir llamadas desde Apache
│       ├── controller/
│       │   ├── AuthController.java  # Endpoints /register y /login
│       │   └── HelloController.java # Endpoint protegido /hello
│       ├── model/
│       │   └── AppUser.java         # Entidad de usuario (email + passwordHash)
│       ├── repo/
│       │   └── AppUserRepository.java
│       └── service/
│           ├── AppUserDetailsService.java  # Carga usuarios desde BD para Spring Security
│           └── AuthService.java            # Registro con BCrypt
└── README.md
```

---

## Endpoints de la API

| Método | Endpoint             | Acceso     | Descripción                       |
|--------|----------------------|------------|-----------------------------------|
| POST   | `/api/auth/register` | Público    | Registra usuario (BCrypt hash)    |
| POST   | `/api/auth/login`    | HTTP Basic | Autentica y retorna confirmación  |
| GET    | `/api/hello`         | HTTP Basic | Endpoint protegido de prueba      |

---

## Seguridad implementada

| Característica      | Implementación                                        |
|---------------------|-------------------------------------------------------|
| TLS end-to-end      | Let's Encrypt en Apache y Spring (PKCS12 keystore)    |
| Hash de contraseñas | BCrypt con salt automático (Spring Security)          |
| Autenticación       | HTTP Basic sobre HTTPS                                |
| Rutas protegidas    | Spring Security — `/register` público, resto privado  |
| CORS                | Configurado para origen Apache únicamente             |
| Base de datos       | H2 embebida con persistencia en archivo               |

---

## Despliegue en AWS — Paso a paso completo

### Requisitos previos

- Dos instancias EC2 en AWS (Amazon Linux 2023, tipo t3.micro)
- Par de llaves `.pem` para SSH
- Dos dominios DuckDNS apuntando a cada instancia:
  - Apache: `ignaciocastillo.duckdns.org` → IP `98.80.74.58`
  - Spring:  `andresrendon.duckdns.org`   → IP `54.234.67.9`
- Security Groups configurados:

| Instancia | Puerto | Descripción        |
|-----------|--------|--------------------|
| Apache    | 22     | SSH                |
| Apache    | 80     | HTTP (→ HTTPS)     |
| Apache    | 443    | HTTPS              |
| Spring    | 22     | SSH                |
| Spring    | 80     | Certbot (temporal) |
| Spring    | 8443   | API HTTPS          |

---

### Despliegue 1 — EC2 Apache (cliente estático)

#### 1. Conectarse a la instancia

```bash
ssh -i LabTDSE.pem ec2-user@ec2-98-80-74-58.compute-1.amazonaws.com
```

#### 2. Instalar Apache

```bash
sudo dnf update -y
sudo dnf install -y httpd
sudo systemctl enable --now httpd
```

#### 3. Instalar Certbot

```bash
sudo dnf install -y certbot python3-certbot-apache
```

#### 4. Subir archivos del cliente (desde tu PC local en PowerShell)

```powershell
# Ejecutar desde la carpeta raíz del proyecto
scp -i LabTDSE.pem client/index.html client/app.js client/styles.css `
    ec2-user@ec2-98-80-74-58.compute-1.amazonaws.com:/home/ec2-user/
```

#### 5. Copiar archivos a la raíz de Apache

```bash
sudo cp /home/ec2-user/index.html  /var/www/html/
sudo cp /home/ec2-user/app.js      /var/www/html/
sudo cp /home/ec2-user/styles.css  /var/www/html/
```

#### 6. Obtener certificado TLS con Let's Encrypt

```bash
sudo certbot --apache -d ignaciocastillo.duckdns.org
```

Cuando pregunte:
- Email: tu correo
- Terms of service: `Y`
- Share email with EFF: `N`
- Virtual host: elegir `1` (ssl.conf)

#### 7. Actualizar la URL de la API en app.js

```bash
sudo nano /var/www/html/app.js
```

Cambiar línea 2:
```js
const API_BASE = "https://andresrendon.duckdns.org:8443";
```

Guardar: `Ctrl+O` → Enter → `Ctrl+X`

#### 8. Reiniciar Apache

```bash
sudo systemctl restart httpd
```

Cliente disponible en: `https://ignaciocastillo.duckdns.org`

---

### Despliegue 2 — EC2 Spring Boot (API REST)

#### 1. Conectarse a la instancia

```bash
ssh -i LabTDSE.pem ec2-user@ec2-54-234-67-9.compute-1.amazonaws.com
```

#### 2. Instalar Java 17, Maven y Git

```bash
sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto maven git
```

#### 3. Clonar el repositorio y compilar

```bash
git clone https://github.com/IgnacioCastillo05/SecureNube.git
cd SecureNube/secure
mvn clean package -DskipTests
```

#### 4. Instalar Certbot y generar certificado TLS

```bash
sudo dnf install -y certbot
sudo certbot certonly --standalone -d andresrendon.duckdns.org
```

Cuando pregunte:
- Email: tu correo
- Terms of service: `Y`
- Share email with EFF: `N`

Los certificados quedan en:
- `/etc/letsencrypt/live/andresrendon.duckdns.org/fullchain.pem`
- `/etc/letsencrypt/live/andresrendon.duckdns.org/privkey.pem`

#### 5. Convertir certificado a PKCS12

```bash
sudo openssl pkcs12 -export \
  -in /etc/letsencrypt/live/andresrendon.duckdns.org/fullchain.pem \
  -inkey /etc/letsencrypt/live/andresrendon.duckdns.org/privkey.pem \
  -out /home/ec2-user/secure-api.p12 \
  -name secure-api \
  -password pass:changeit

sudo chmod 644 /home/ec2-user/secure-api.p12
```

#### 6. Configurar application.properties con TLS

```bash
cat > ~/SecureNube/secure/src/main/resources/application.properties << 'EOF'
server.port=8443

spring.datasource.url=jdbc:h2:file:./data/secure-app
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

server.ssl.enabled=true
server.ssl.key-store-type=PKCS12
server.ssl.key-store=/home/ec2-user/secure-api.p12
server.ssl.key-store-password=changeit
server.ssl.key-alias=secure-api
EOF
```

#### 7. Recompilar con la nueva configuración

```bash
cd ~/SecureNube/secure
mvn clean package -DskipTests
```

#### 8. Ejecutar la aplicación en background

```bash
nohup java -jar target/secure-0.0.1-SNAPSHOT.jar > ~/app.log 2>&1 &
echo $! > ~/app.pid
```

#### 9. Verificar que arrancó correctamente

```bash
tail -20 ~/app.log
```

Debe aparecer:
```
Started Application in X seconds
Tomcat started on port 8443 (https)
```

#### 10. Verificar la API con curl

```bash
curl -k https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}'
```

Respuesta esperada: `{"status":"registered"}`

API disponible en: `https://andresrendon.duckdns.org:8443`

---

## Prueba del flujo completo

1. Abre `https://ignaciocastillo.duckdns.org` en el navegador
2. **Registro** → ingresa email y contraseña → *Crear cuenta* → debe responder `{"status":"registered"}`
3. **Login** → mismas credenciales → *Iniciar sesión* → debe responder `{"status":"authenticated"}`
4. **API protegida** → *Llamar /api/hello* → debe responder `{"message":"Hola desde el API seguro de Spring!"}`

---

## Comandos útiles

```bash
# Ver logs de Spring en tiempo real
tail -f ~/app.log

# Detener Spring
kill $(cat ~/app.pid)

# Reiniciar Apache
sudo systemctl restart httpd

# Verificar estado de Apache
sudo systemctl status httpd

# Renovar certificados Let's Encrypt
sudo certbot renew --dry-run
```

---

## Solución de problemas

| Problema | Causa probable | Solución |
|---|---|---|
| `Failed to fetch` en el browser | Puerto 8443 cerrado | Abrir puerto en Security Group de Spring |
| Certbot falla con Timeout | Puerto 80 cerrado | Abrir puerto 80 en Security Group |
| Spring no arranca (Permission denied) | Permisos del .p12 | `sudo chmod 644 ~/secure-api.p12` |
| CSS no carga | Nombre incorrecto del archivo | Verificar que sea `styles.css` no `style.css` |
| `Could not load store` | Ruta incorrecta del keystore | Verificar ruta en `application.properties` |
| DuckDNS no resuelve | DNS no propagado | Esperar unos minutos y verificar con `nslookup` |

---

## Referencias

- [AWS EC2 LAMP Guide — Amazon Linux 2023](https://docs.aws.amazon.com/linux/al2023/ug/ec2-lamp-amazon-linux-2023.html)
- [Spring Boot Security](https://docs.spring.io/spring-security/reference/)
- [Let's Encrypt — Certbot](https://certbot.eff.org/)
- [DuckDNS — DNS dinámico gratuito](https://www.duckdns.org/)
- Taller: *Arquitectura Segura* — Luis Daniel Benavides Navarro, 2020



Link Video: [text](https://1drv.ms/v/c/984f273dedec22e6/IQB6uJ41eXduT78B8n_-6k6xAbNYhi3dr2gg3cz7xS2pTSE?e=ozOoZJ)