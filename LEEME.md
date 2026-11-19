# Kaisen 改善: app Android + web

Tiene entrenos, cronómetro, hidratación, peso y estadísticas. Toda la app está en
`app/src/main/assets/index.html`. Android la muestra en un WebView y GitHub Pages la publica como página web.

---

## 1. Obtener el APK (GitHub, sin instalar nada)
1. Crea un repositorio **público** en github.com, por ejemplo `kaisen`.
   Tiene que ser público porque GitHub Pages es gratis solo en repositorios públicos.
2. Sube **todo** el contenido de esta carpeta, incluida la carpeta oculta `.github`.
3. En **Actions**, el flujo "Compilar APK" se ejecuta solo (tarda unos 3–5 min).
4. Abre la ejecución, descarga **Kaisen-apk** (.zip), saca el `.apk` y pásalo a tu teléfono.
5. Ábrelo y permite "Instalar apps desconocidas".

Las versiones nuevas se instalan **encima** de la anterior y conservan tus datos,
porque siempre se firman con la misma llave (`app/pulso.keystore`).

## 2. Versión web (para la computadora)
1. En el repositorio: **Settings → Pages → Source: GitHub Actions**.
2. Vuelve a ejecutar "Publicar web" en Actions, o sube cualquier cambio.
3. Tu app queda en `https://TU_USUARIO.github.io/kaisen/`.

---

## 3. Cuenta de Google y sincronización (Firebase, gratis)
Sin este paso la app funciona igual, pero tus datos solo se guardan en cada dispositivo.

### 3.1 Crear el proyecto
1. Entra a https://console.firebase.google.com, pulsa **Crear un proyecto** y ponle el nombre `kaisen`.
   Puedes desactivar Analytics.
2. **Compilación → Authentication → Comenzar → Método de acceso**:
   - Habilita **Correo electrónico/contraseña**.
   - Habilita **Google** y elige tu correo de asistencia. Dentro de Google abre
     **"Configuración del SDK web"** y copia el **ID de cliente web**
     (termina en `.apps.googleusercontent.com`).
3. **Compilación → Firestore Database → Crear base de datos**, en modo producción y con la región más cercana.
   En la pestaña **Reglas** pega esto y pulsa **Publicar**:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
   Con estas reglas, cada usuario solo puede leer y escribir sus propios datos.

### 3.2 Registrar la app web
1. Ve a ⚙️ **Configuración del proyecto → General → Tus apps** y pulsa el icono **</>** (Web).
   Ponle de nombre `Kaisen web`. No necesitas Hosting.
2. Copia los valores de `firebaseConfig` y pégalos en `app/src/main/assets/index.html`,
   donde dice `const FIREBASE_CONFIG = null;`. Debe quedar así:
   ```js
   const FIREBASE_CONFIG = {
     apiKey: "AIza...",
     authDomain: "pulso-xxxx.firebaseapp.com",
     projectId: "pulso-xxxx",
     appId: "1:123...:web:abc...",
     googleWebClientId: "123...-abc.apps.googleusercontent.com"
   };
   ```
   Estos valores **no son secretos**: Firebase los diseña para ir dentro de la app.
   A tus datos los protegen las reglas del paso 3.1.

### 3.3 Registrar la app Android (para "Continuar con Google" en la APK)
1. En **Tus apps** pulsa **Agregar app → Android**.
2. Nombre del paquete: `com.arakyel.pulso`
3. Certificado SHA-1:
   `D7:AB:CD:35:D2:BC:6B:19:77:09:8E:68:67:0C:71:93:68:1C:6F:B1`
4. Pulsa **Registrar app**. No necesitas descargar `google-services.json`; puedes saltar los demás pasos.

### 3.4 Autorizar tu página web
**Authentication → Configuración → Dominios autorizados → Agregar dominio**: escribe `TU_USUARIO.github.io`

### 3.5 Listo
Sube el `index.html` editado a GitHub. Se vuelven a generar el APK y la página web.
Para crear tu usuario: abre **Ajustes → Usuario** y elige "Continuar con Google" o "Crear cuenta".

> Atajo para probar en un solo dispositivo: en **Ajustes → Conectar Firebase** puedes pegar
> la configuración sin editar el archivo. Solo aplica en ese dispositivo.

---

## Cómo funciona la sincronización
- Cada cambio se guarda al instante en el teléfono y a los pocos segundos se sube a tu cuenta.
- Al abrir la app en otro dispositivo, descarga lo último.
- La primera vez que inicias sesión, si el dispositivo y la nube tienen datos distintos,
  la app te pregunta cuáles conservar.
- Sin internet todo sigue funcionando. Los cambios se suben al volver a abrir la app con conexión.

## Notas
- La llave `app/pulso.keystore` sirve para instalar la app tú mismo. Para publicar en Google Play haría falta una llave privada.
- Requiere Android 8.0 o superior y Google Play Services, que es necesario para iniciar sesión con Google.

- El paquete interno sigue siendo `com.arakyel.pulso` para que la app se actualice encima de la versión anterior sin perder datos. El nombre que ves en el teléfono es **Kaisen**.
