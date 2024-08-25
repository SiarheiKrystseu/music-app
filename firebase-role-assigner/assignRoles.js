const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const auth = admin.auth();

// Define users and their roles
const users = {
  'admin@email.com': ['admin', 'user'],
  'user1@email.com': ['user']
};

async function assignRoles() {
  try {
    for (const [email, roles] of Object.entries(users)) {
      // Fetch user by email
      const user = await auth.getUserByEmail(email);
      const uid = user.uid;

      // Assign custom claims
      await auth.setCustomUserClaims(uid, { roles });
      console.log(`Assigned roles ${roles.join(', ')} to user ${email}`);
    }
  } catch (error) {
    console.error('Error assigning roles:', error);
  }
}

assignRoles();
