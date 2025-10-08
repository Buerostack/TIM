#!/usr/bin/env node

/**
 * TIM 2.0 - Basic JWT Usage Example
 *
 * This example demonstrates:
 * 1. Generating custom JWT tokens
 * 2. Decoding and validating tokens
 * 3. Listing user tokens
 * 4. Extending token expiration
 * 5. Making authenticated requests
 * 6. Revoking tokens
 */

require('dotenv').config();
const axios = require('axios');
const jwt = require('jsonwebtoken');

const TIM_API_URL = process.env.TIM_API_URL || 'http://localhost:8085';

// ANSI color codes for pretty output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  blue: '\x1b[34m',
  yellow: '\x1b[33m',
  red: '\x1b[31m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function step(number, description) {
  console.log(`\n${colors.green}✅ Step ${number}: ${description}${colors.reset}`);
}

async function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
  let token = null;
  let tokenId = null;

  try {
    log('\n🚀 TIM 2.0 - Basic JWT Usage Example', 'bright');
    log('━'.repeat(40), 'blue');

    // Step 1: Generate JWT Token
    step(1, 'Generate JWT Token');
    const generateResponse = await axios.post(`${TIM_API_URL}/jwt/custom/generate`, {
      JWTName: 'example-token',
      content: {
        sub: 'user123',
        role: 'user',
        email: 'user@example.com',
        customClaim: 'custom-value'
      },
      expirationInMinutes: 60
    });

    token = generateResponse.data.token;
    tokenId = generateResponse.data.tokenId;
    const expiresAt = generateResponse.data.expiresAt;

    log(`   Token ID: ${tokenId}`, 'yellow');
    log(`   Expires: ${expiresAt}`, 'yellow');
    log(`   Token: ${token.substring(0, 50)}...`, 'yellow');

    await wait(500);

    // Step 2: Decode Token (Client-Side)
    step(2, 'Decode Token (Client-Side)');
    const decoded = jwt.decode(token, { complete: true });
    log(`   Header: ${JSON.stringify(decoded.header)}`, 'yellow');
    log(`   Payload: ${JSON.stringify(decoded.payload, null, 2)}`, 'yellow');

    await wait(500);

    // Step 3: List User Tokens
    step(3, 'List User Tokens');
    const listResponse = await axios.post(
      `${TIM_API_URL}/jwt/custom/list/me`,
      {},
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    log(`   Found ${listResponse.data.length} token(s)`, 'yellow');
    if (listResponse.data.length > 0) {
      const firstToken = listResponse.data[0];
      log(`   Latest: ${firstToken.jwtName} (${firstToken.status})`, 'yellow');
    }

    await wait(500);

    // Step 4: Extend Token Expiration
    step(4, 'Extend Token Expiration');
    const extendResponse = await axios.post(
      `${TIM_API_URL}/jwt/custom/extend`,
      {
        tokenId: tokenId,
        extensionInMinutes: 30
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    log(`   New expiration: ${extendResponse.data.expiresAt}`, 'yellow');
    log(`   Extended by: 30 minutes`, 'yellow');

    await wait(500);

    // Step 5: Make Authenticated Request
    step(5, 'Make Authenticated Request');
    const authResponse = await axios.post(
      `${TIM_API_URL}/jwt/custom/list/me`,
      {},
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    log(`   Request successful!`, 'yellow');
    log(`   User tokens: ${authResponse.data.length}`, 'yellow');

    await wait(500);

    // Step 6: Revoke Token
    step(6, 'Revoke Token');
    const revokeResponse = await axios.post(
      `${TIM_API_URL}/jwt/custom/revoke`,
      {
        tokenId: tokenId
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    log(`   Token revoked successfully`, 'yellow');
    log(`   Status: ${revokeResponse.data.status}`, 'yellow');

    await wait(500);

    // Summary
    log('\n━'.repeat(40), 'blue');
    log('✨ Example completed successfully!', 'green');
    log('━'.repeat(40) + '\n', 'blue');

  } catch (error) {
    log('\n❌ Error occurred:', 'red');
    if (error.response) {
      log(`   Status: ${error.response.status}`, 'red');
      log(`   Message: ${JSON.stringify(error.response.data)}`, 'red');
    } else if (error.request) {
      log('   No response received from TIM API', 'red');
      log(`   Is TIM running at ${TIM_API_URL}?`, 'red');
    } else {
      log(`   ${error.message}`, 'red');
    }
    process.exit(1);
  }
}

// Run the example
if (require.main === module) {
  main();
}

module.exports = { main };
