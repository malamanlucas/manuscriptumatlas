'use strict'

exports.config = {
  app_name: [process.env.NEW_RELIC_APP_NAME || 'manuscriptum-atlas-frontend-prod'],
  license_key: process.env.NEW_RELIC_LICENSE_KEY || 'a3728675fbf7fd64e0905dff3007eb520eccNRAL',
  distributed_tracing: { enabled: true },
  logging: { level: 'info', filepath: 'stdout' },
  allow_all_headers: true,
  attributes: { exclude: ['request.headers.cookie', 'request.headers.authorization'] },
}
