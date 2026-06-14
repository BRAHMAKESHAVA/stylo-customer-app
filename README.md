# Stylo Customer Application

The **Stylo Customer Application** integrates with Firebase Cloud Messaging (FCM) to deliver real‑time notifications to customers and partners.

---

## Firebase Setup

For security reasons, Firebase service account credentials are **not stored in the repository**.  
Each developer must configure their own local Firebase credentials.

### Local Development Setup

1. **Download Firebase Service Account JSON**  
   - From [Firebase Console](https://console.firebase.google.com/) → Project Settings → Service Accounts → Generate New Private Key.

2. **Store the file securely**  
   - Do not commit it to the repository.  
   - Recommended paths:  
     - Windows: `C:\Secrets\firebase-service-account.json`  
     - Linux/Mac: `/opt/secrets/firebase-service-account.json`

3. **Configure environment variable**

   - Windows:
     ```powershell
     setx FIREBASE_CREDENTIALS_PATH "C:\Secrets\firebase-service-account.json"
     ```
   - Linux/Mac:
     ```bash
     export FIREBASE_CREDENTIALS_PATH=/opt/secrets/firebase-service-account.json
     ```

4. **Restart IDE/Terminal** to apply the environment variable.

---

### application.yml Configuration

```yaml
firebase:
  credentials-path: ${FIREBASE_CREDENTIALS_PATH}
