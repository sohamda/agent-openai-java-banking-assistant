metadata description = 'Creates a VNet with subnets and private endpoints for Cognitive Services accounts.'

param location string = resourceGroup().location
param tags object = {}

@description('Name of the Virtual Network.')
param vnetName string

@description('Name of the private endpoint for the OpenAI account.')
param openAiPrivateEndpointName string

@description('Name of the private endpoint for the Document Intelligence account.')
param documentIntelligencePrivateEndpointName string

@description('Resource ID of the Azure OpenAI Cognitive Services account.')
param openAiAccountId string

@description('Resource ID of the Document Intelligence Cognitive Services account.')
param documentIntelligenceAccountId string

var containerAppsSubnetName = 'snet-apps'
var privateEndpointSubnetName = 'snet-pe'

resource vnet 'Microsoft.Network/virtualNetworks@2023-05-01' = {
  name: vnetName
  location: location
  tags: tags
  properties: {
    addressSpace: {
      addressPrefixes: [
        '10.0.0.0/16'
      ]
    }
    subnets: [
      {
        name: containerAppsSubnetName
        properties: {
          addressPrefix: '10.0.0.0/21'
          delegations: [
            {
              name: 'Microsoft.App.environments'
              properties: {
                serviceName: 'Microsoft.App/environments'
              }
            }
          ]
        }
      }
      {
        name: privateEndpointSubnetName
        properties: {
          addressPrefix: '10.0.8.0/24'
          privateEndpointNetworkPolicies: 'Enabled'
        }
      }
    ]
  }
}

var peSubnetId = '${vnet.id}/subnets/${privateEndpointSubnetName}'

resource openAiPrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = {
  name: openAiPrivateEndpointName
  location: location
  tags: tags
  properties: {
    subnet: {
      id: peSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'openai-connection'
        properties: {
          privateLinkServiceId: openAiAccountId
          groupIds: [
            'account'
          ]
        }
      }
    ]
  }
}

resource documentIntelligencePrivateEndpoint 'Microsoft.Network/privateEndpoints@2023-05-01' = {
  name: documentIntelligencePrivateEndpointName
  location: location
  tags: tags
  properties: {
    subnet: {
      id: peSubnetId
    }
    privateLinkServiceConnections: [
      {
        name: 'docint-connection'
        properties: {
          privateLinkServiceId: documentIntelligenceAccountId
          groupIds: [
            'account'
          ]
        }
      }
    ]
  }
}

resource openAiDnsZone 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.openai.azure.com'
  location: 'global'
  tags: tags
}

resource cogServicesDnsZone 'Microsoft.Network/privateDnsZones@2020-06-01' = {
  name: 'privatelink.cognitiveservices.azure.com'
  location: 'global'
  tags: tags
}

resource openAiDnsZoneVnetLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: openAiDnsZone
  name: 'openai-vnet-link'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnet.id
    }
    registrationEnabled: false
  }
}

resource cogServicesDnsZoneVnetLink 'Microsoft.Network/privateDnsZones/virtualNetworkLinks@2020-06-01' = {
  parent: cogServicesDnsZone
  name: 'cogservices-vnet-link'
  location: 'global'
  properties: {
    virtualNetwork: {
      id: vnet.id
    }
    registrationEnabled: false
  }
}

resource openAiDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-05-01' = {
  parent: openAiPrivateEndpoint
  name: 'openai-dns-zone-group'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'openai-config'
        properties: {
          privateDnsZoneId: openAiDnsZone.id
        }
      }
    ]
  }
}

resource docIntDnsZoneGroup 'Microsoft.Network/privateEndpoints/privateDnsZoneGroups@2023-05-01' = {
  parent: documentIntelligencePrivateEndpoint
  name: 'docint-dns-zone-group'
  properties: {
    privateDnsZoneConfigs: [
      {
        name: 'cogservices-config'
        properties: {
          privateDnsZoneId: cogServicesDnsZone.id
        }
      }
    ]
  }
}

output containerAppsSubnetId string = '${vnet.id}/subnets/${containerAppsSubnetName}'
output vnetId string = vnet.id
