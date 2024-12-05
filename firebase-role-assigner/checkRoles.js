const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const auth = admin.auth();

// Replace with the email address of the user you want to check
const email = 'user1@email.com';

async function checkRoles() {
  try {
    // Fetch user by email
    const user = await auth.getUserByEmail(email);
    const uid = user.uid;

    // Fetch custom claims for the user
    const userRecord = await auth.getUser(uid);
    console.log(`Custom claims for ${email}:`, userRecord.customClaims);
  } catch (error) {
    console.error('Error fetching custom claims:', error);
  }
}

checkRoles();
