metadata description = 'Creates a Virtual Network with subnets for Container Apps infrastructure and private endpoints.'
param name string
param location string = resourceGroup().location
param tags object = {}

@description('Address prefix for the virtual network')
param addressPrefix string = '10.0.0.0/16'

@description('Address prefix for the Container Apps infrastructure subnet')
param infrastructureSubnetPrefix string = '10.0.0.0/23'

@description('Address prefix for the private endpoints subnet')
param privateEndpointsSubnetPrefix string = '10.0.2.0/27'

resource vnet 'Microsoft.Network/virtualNetworks@2023-04-01' = {
  name: name
  location: location
  tags: tags
  properties: {
    addressSpace: {
      addressPrefixes: [
        addressPrefix
      ]
    }
    subnets: [
      {
        name: 'infrastructure-subnet'
        properties: {
          addressPrefix: infrastructureSubnetPrefix
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
        name: 'private-endpoints-subnet'
        properties: {
          addressPrefix: privateEndpointsSubnetPrefix
          privateEndpointNetworkPolicies: 'Disabled'
        }
      }
    ]
  }

  resource infrastructureSubnet 'subnets' existing = {
    name: 'infrastructure-subnet'
  }

  resource privateEndpointsSubnet 'subnets' existing = {
    name: 'private-endpoints-subnet'
  }
}

output vnetId string = vnet.id
output vnetName string = vnet.name
output infrastructureSubnetId string = vnet::infrastructureSubnet.id
output privateEndpointsSubnetId string = vnet::privateEndpointsSubnet.id
