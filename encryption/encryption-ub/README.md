# Unbound Key Control

Unbound Key Control ("UKC") protects secrets such as cryptographic keys by ensuring they never exist in complete form.


<a name="Prerequisites"></a>
## Prerequisites
- Install UKC (EP, Partner, and Auxiliary servers) version 2.0.2001 and up.
- Using the EP server, create the  UKC partition that will store keys used by the Tessera server (referred below as the PARTITION_NAME).


## Preparing Tessera Server

To allow using the UKC cryptographic services, the UKC SSL client certificate and its SSL trust CA certificate must be installed on the Tessera server, and the environment variables used by the UKC software must be set accordingly.

The UKC certificates may be obtained using one of the following methods:

1. Explicitly create the certificate on the EP server - [Create and Download the Certificate](#FullCert).
1. Obtain an ephemeral certificate from the server - [Obtain Ephemeral Certificate](#Ephemeral).
1. Install UKC client software on the Tessera server and use the standard UKC client registration procedure to obtain the  certificates - [Install with a UKC Client](#Withclient).

<a name="FullCert"></a>
### Option one: Create and Download the Certificate
1. To create the certificate, run the following command on the EP server.

   Use the <PARTITION_NAME> assigned in [Prerequisites](#Prerequisites).
   Specify the SO password that allows accessing  the partition in  the <PARTITION_PASSWORD>
   Specify Tessara's hostname in the <TESSERA_HOST_NAME>. This value will be included in the certificate.
   In the --output option, specify the name of the certificate file, for example, "tessera-client.pfx".
   Set the password that protects the content of the certificate in the <PFX_PASSWORD>.
    
    ```
    ucl client create --mode FULL --partition <PARTITION_NAME> --password <PARTITION_PASSWORD> --name <TESSERA_HOST_NAME> --output ./tessera_client.pfx --pfx_password <PFX_PASSWORD>
    ```
   By default, this certificate is valid for three years. To change the default, append the following option:
    
    ```
    --cert_validity <Validity period of each derived certificate>
    ```
    
   
1. To obtain the UKC SSL trust certificate (ukc_ca.p7b) run the following command on the EP server.
   
   ```
    ucl root_ca --output ./ukc_ca.p7b
    ```   

1. Upload these two files to the Tessera server.


1. Configure Environment Variables on Tessera server

    The following environment variables need to be configured:

    ```
    UKC_SERVERS=<EP_HOSTNAME>
    UKC_PARTITION_NAME=<PARTITION_NAME>
    DYLOG_ENABLED=1
    UKC_CA=<path-to-ukc_ca.p7b>
    UKC_PFX=<path-to-tessera_client.pfx>
    UKC_PFX_PASS=<PFX_PASSWORD>
    ```

<a name="Ephemeral"></a>
### Option two: Obtain Ephemeral Certificate
This option has the following advantages

- On the EP server, you create a template that is used to derive certificates. You specify for how long this template is valid and its access credentials. As long as you know the credentials (name and access code) you can use it from any UKC service client multiple times without the further need to manage the EP server.

- You obtain the certificate for the specific period in the granularity of minutes. Once this period expires you may obtain the certificate for another period and so forth based on your requirements. For example, you can obtain the certificate on demand for the fixed period, or schedule its availability in advance.

The control is totally on your side without any further engagement with the UKC server admin.


1. To create the certificate template, run the following command on the EP server.

   Use the <PARTITION_NAME> assigned in [Prerequisites](#Prerequisites).
   Specify the SO password that allows accessing  the partition in  the <PARTITION_PASSWORD>
   Specify Tessara's hostname in the <TESSERA_HOST_NAME>. This value will be included in the certificate.
   In the --output option, specify the name of the certificate file, for example, "tessera-client.pfx".
   Set the password that protects the content of the certificate in the <PFX_PASSWORD>.
    

    Run this command to create a UKC client on the UKC EP. The result is an *Activation Code* used in the next step.
    
    ```
    ucl  client create --mode template --name <TEMPLATE_NAME> --partition <PARTITION_NAME> --password <UKC_PASSWORD>
    ```
    
    By default, this template is valid for 30 minutes and the certificates derived from it are valid for 20 minutes. To change the defaults, add the following options:
    
    ```
    --ac_validity <The template validity period in minutes>
    --cert_validity <Validity period of each derived certificate>
    ```
    
    The output of this command is <ACTIVATION_CODE>. Together with the <TEMPLATE_NAME> they will let Tessera to obtain its SSL clinet certificate.
    
 1. To obtain the UKC SSL trust certificate (ukc_ca.p7b) run the following command on the EP server.
   
    ```
    ucl root_ca --output ./ukc_ca.p7b
    ```   

1. Upload this file to the Tessera server.

    
1. Configure Environment Variables on Tessera server

    The following environment variables need to be configured:

    ```
    UKC_SERVERS=<EP_HOSTNAME>
    UKC_PARTITION_NAME=<PARTITION_NAME>
    DYLOG_ENABLED=1
    UKC_CA=<path-to-ukc_ca.p7b>
    UKC_TEMPLATE_NAME=<TEMPLATE_NAME>
    UKC_ACTIVATION_CODE=<ACTIVATION_CODE>

    ```
<a name="Withclient"></a>
### Option three: Use the UKC Client Software

If you installed the UKC Client Software on the Tessera server (refer to [UKC User Guide](https://www.unboundtech.com/docs/UKC/UKC_User_Guide/HTML/Content/Products/UKC-EKM/UKC_User_Guide/Installation/ClientInstallation.html)), you can choose the following standard UKC client creation and registration approach to implicitly obtain the required certificates.


1. To create the certificate template, run the following command on the EP server.

    ```
    ucl client create --mode activate --name <CLIENT_NAME> --partition <PARTITION_NAME> --password <UKC_PASSWORD>
    ```
1. On the Terserra server. Edit the configuration file on the UKC client, found in:

    `/etc/ekm/client.conf`
    
    Update the server name of the UKC EP. For example:
    
    `servers=<EP_HOSTNAME>`
1. On the Terera server. Obtain the necessary certificates
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
