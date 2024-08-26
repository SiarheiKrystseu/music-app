const admin = require('firebase-admin');

// Initialize Firebase Admin SDK with your service account
const serviceAccount = require('./serviceAccountKey.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const auth = admin.auth();

// Function to generate a custom token for a user
async function generateCustomToken(uid) {
  try {
    const customToken = await auth.createCustomToken(uid);
    console.log('Custom Token:', customToken);
    return customToken;
  } catch (error) {
    console.error('Error creating custom token:', error);
    throw error;
  }
}

// Main function to generate custom token
async function main() {
  //const uid = 'HdmMnvj1nJaeQMaYcMinympxXny2'; // Admin user
  const uid = 'EyLDifcNeGbVyJMXwSezvfnL8Q53'; // User1

  try {
    // Generate custom token
    const customToken = await generateCustomToken(uid);

    // Normally, you would send this customToken to the client
    // For this script, we are just logging it
    console.log('Generated Custom Token:', customToken);
  } catch (error) {
    console.error('Error during token generation:', error);
  }
}

// Execute the main function
main();
