const fs = require('fs');
const path = require('path');

const gradlePropertiesPath = path.join(__dirname, '../gradle.properties');

// Get the new version from the command line arguments
const newVersion = process.argv[2];

if (!newVersion) {
    console.error('Error: Version number is required');
    process.exit(1);
}

// Read the gradle.properties file
let gradleProperties = fs.readFileSync(gradlePropertiesPath, 'utf8');

// Update the version property
gradleProperties = gradleProperties.replace(/version=.*/, `version=${newVersion}`);

// Write the updated gradle.properties back to disk
fs.writeFileSync(gradlePropertiesPath, gradleProperties, 'utf8');

console.log(`Updated gradle.properties to version ${newVersion}`);