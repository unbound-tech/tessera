# Unbound Key Control

Unbound Key Control ("UKC") protects secrets such as cryptographic keys by ensuring they never exist in complete form.

There are two types of installation:
1. [Clientless Installation](#Clientless)
1. [Install with a UKC Client](#Withclient)

## Prerequisites
- Install UKC (EP, Partner and Auxiliary servers) version 2.0.2001 and up.

<a name="Clientless"></a>
## Clientless Installation
### Option one to create clientless with pfx
To install clientless encryption, execute the following on the same server as Tessera.
1. Create UKC Client

    Run this command to create a UKC client on the UKC EP. The result is a *PFX* file used in the next step.
    
    ```
    ucl client create --mode full --name <CLIENT_NAME> --partition <PARTITION_NAME> --password <UKC_PASSWORD>  --pfx_password <PFX_PASSWORD> --output tessera-client.pfx
    ```

2. Server CA

    Copy the server CA from the server to the client device. It can be found in:
    
    `/etc/ekm/server-ca.p7b`

3. Configure Environment Variables on Tessera server

    The following environment variables need to be configured:

    ```
    UKC_CA=/path-to-file/server-ca.p7b
    UKC_PARTITION_NAME=<PARTITION_NAME>
    UKC_SERVERS=<EP_HOSTNAME>
    DYLOG_ENABLED=1
    UKC_PFX=<path-to-pfx-file>
    UKC_PFX_PASS=<PFX_PASSWORD>
    ```
### Option two to create clientless without pfx
1. Create UKC Client

    Run this command to create a UKC client on the UKC EP. The result is a *Activition Code* used in the next step.
    
    ```
    ucl  client create --mode template --name <CLIENT_NAME> --partition <PARTITION_NAME> --password <UKC_PASSWORD>
    ```
2. Server CA

    Copy the server CA from the server to the client device. It can be found in:
    
    `/etc/ekm/server-ca.p7b`
    
3. Configure Environment Variables on Tessera server

    The following environment variables need to be configured:

    ```
    UKC_CA=/path-to-file/server-ca.p7b
    UKC_PARTITION_NAME=<PARTITION_NAME>
    UKC_SERVERS=<EP_HOSTNAME>
    DYLOG_ENABLED=1
    UKC_ACTIVATION_CODE=<ACTIVATION_CODE>
    UKC_TEMPLATE_NAME=<CLIENT_NAME>
    ```
<a name="Withclient"></a>
## Install with a UKC Client
Install the UKC client on the same server as Tessera.

1. Install UKC client.

    Follow the instructions in the [UKC User Guide](https://www.unboundtech.com/docs/UKC/UKC_User_Guide/HTML/Content/Products/UKC-EKM/UKC_User_Guide/Installation/ClientInstallation.html).
2. Configure the UKC EP server.   
    ```
    ucl client create --mode activate --name <CLIENT_NAME> --partition <PARTITION_NAME> --password <UKC_PASSWORD>
    ```
3. Edit the configuration file on the UKC client, found in:

    `/etc/ekm/client.conf`
    
    Update the server name of the UKC EP. For example:
    
    `servers=<EP_HOSTNAME>`
4. Configure the UKC client.
    ```
    ucl register --code <ACTIVATION_CODE> --name <CLIENT_NAME> --partition <PARTITION_NAME> -v
    ```

## Using UKC for Encryption with Tessera

1. Create the key and public key.
    ```
    java -jar tessera-app.jar -keygen --encryptor.type UB -filename <FILENAME>
    ```
    This command creates 2 files:
    - `<FILENAME>.key`
    - `<FILENAME>.pub`
		
2. Create the Tessera configuration file.

    Create a file containing the Tessera configuration information. See [here](https://github.com/jpmorganchase/quorum-examples#experimenting-with-alternative-curves-in-tessera) for more information.

    Set the *encryptor* type to *UB*:
    ```
    "encryptor": {
        "type": "UB"
    },
    ```

    Set the paths to the keys:
    ```
	"keys": {
        "passwords": [],
        "keyData": [
            {
                "config": "$(cat $<FILENAME>.key)",
                "publicKey": "$(cat $<FILENAME>.pub)"
            }
        ]
    },	
   ```
