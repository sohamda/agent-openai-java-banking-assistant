metadata description = 'Creates an Azure Key Vault.'
param name string
param location string = resourceGroup().location
param tags object = {}

@description('Allow the key vault to be used for template deployment.')
param enabledForDeployment bool = false

@description('Enable soft delete for the key vault.')
param enableSoftDelete bool = true

@description('Enable purge protection for the key vault.')
param enablePurgeProtection bool = true

@description('Enable RBAC authorization for the key vault.')
param enableRbacAuthorization bool = true

@description('Number of days to retain soft-deleted secrets.')
@minValue(7)
@maxValue(90)
param softDeleteRetentionInDays int = 90

resource keyVault 'Microsoft.KeyVault/vaults@2022-07-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: { family: 'A', name: 'standard' }
    enabledForDeployment: enabledForDeployment
    enableSoftDelete: enableSoftDelete
    enablePurgeProtection: enablePurgeProtection
    enableRbacAuthorization: enableRbacAuthorization
    softDeleteRetentionInDays: softDeleteRetentionInDays
  }
}

output endpoint string = keyVault.properties.vaultUri
output id string = keyVault.id
output name string = keyVault.name
