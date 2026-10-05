# Setup Firebase App Distribution for TaskFlow

This guide explains how Firebase App Distribution is configured and the quick steps you need to take on the Firebase Console to distribute updates automatically to your WhatsApp group / testers.

---

## What We Have Configured in Your Codebase
1. **GitHub Actions CI/CD (`.github/workflows/distribution.yml`)**:
   - Automatically builds your debug APK whenever you push to `main` or trigger the workflow manually.
   - Saves the downloadable APK in GitHub Actions artifacts.
   - Automatically deploys the APK to **Firebase App Distribution** tester group (`testers`) as soon as you add the Firebase secrets to your GitHub repository!
2. **`.gitignore` Protection**:
   - Kept secure against leaking credentials (`google-services.json`, `firebase-app-distribution-key.json`, `*.jks`, etc.).

---

## What Remains For You To Do (Step-by-Step)

### Step 1: Create a Firebase Project
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or **Create a project**).
3. Name it (e.g. `TaskFlow-Todo` or `Todo-List-App`) and continue (you can disable Google Analytics or leave it on).

---

### Step 2: Register your Android App in Firebase
1. On your Firebase Project dashboard, click the **Android** icon (or go to **Project settings** > **Add app**).
2. Enter your Package Name (Application ID):
   ```text
   com.example.todo_list
   ```
3. Enter an App nickname (e.g., `TaskFlow Debug`).
4. Click **Register app**.
5. Copy your **App ID** (looks like: `1:1234567890:android:abcdef123456`). *(You will need this!)*
6. You can skip downloading `google-services.json` unless you plan to use Firebase Analytics/Crashlytics inside the app.

---

### Step 3: Set Up App Distribution & Add Your Testers
1. In the Firebase console left menu, go to **Release & Monitor** > **App Distribution**.
2. Click **Get started**.
3. Go to the **Testers & Groups** tab.
4. Click **Add group**, name it:
   ```text
   testers
   ```
5. Add the email addresses of your friends and sir to this group.
   > **Note:** Each tester will receive an email invitation to accept. Once accepted, they install the "App Distribution" tester app on their phone. From that point on, whenever you push a build, they get a notification and can update in one tap!

---

### Step 4: (Zero-Touch Automation) Connect GitHub to Firebase
To let GitHub automatically upload your APK whenever you push code:

1. **Create a Service Account Key in Google Cloud / Firebase**:
   - In Firebase Console, go to **Project Settings** (gear icon) > **Service accounts**.
   - Click **Manage service account permissions** (or go to Google Cloud Console IAM & Admin).
   - Ensure the service account has the **Firebase App Distribution Admin** role.
   - Under **Keys**, click **Add Key** > **Create new key** > Choose **JSON** > Download the JSON file.
2. **Add Secrets to GitHub**:
   - Go to your GitHub repository: `https://github.com/PrerakPithadiya/android-compose-todo/settings/secrets/actions`
   - Click **New repository secret**:
     - Name: `FIREBASE_APP_ID`
     - Value: *(Your Firebase Android App ID from Step 2, e.g. `1:1234567890:android:...`)*
   - Click **New repository secret** again:
     - Name: `FIREBASE_SERVICE_CREDENTIALS`
     - Value: *(Open the downloaded JSON key file in Notepad, copy the whole JSON text, and paste it here)*

---

### Alternative: Upload Manually Without Any Cloud Setup!
If you don't want to set up GitHub Actions secrets right away:
1. Whenever you build your APK (or let GitHub Actions build it):
2. Go to **Firebase Console** > **App Distribution**.
3. Simply **drag and drop** your `app-debug.apk` directly into the web browser!
4. Select the `testers` group, type a short message (e.g. *"Fixed swipe-to-delete bug"*), and click **Distribute**.
5. All your testers get an instant notification on their phones!
