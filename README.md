[![Maven Central](https://img.shields.io/maven-central/v/com.fathzer/jclop-dropbox2)](https://central.sonatype.com/artifact/com.fathzer/jclop-dropbox2)
<picture>
  <img alt="License" src="https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg">
</picture>
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jclop2_dropbox&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=jclop2_dropbox)
[![javadoc](https://javadoc.io/badge2/com.fathzer/jclop-dropbox2/javadoc.svg)](https://javadoc.io/doc/com.fathzer/jclop-dropbox2)

# jclop2-dropbox
A [JClop](https://github.com/jclop2/JClop) implementation that uses Dropbox as underlying storage provider.

It requires java 8+.

Limitations : sub-folders are not supported in Swing file chooser.

## Usage example

### Pre-requisite
In order to connect your application with Dropbox, you should [register it](https://www.dropbox.com/developers/apps/create) in the [Developer section](https://www.dropbox.com/developers/) of the Dropbox web site.
Once the application has been created, you will get an application key and an application secret. Both are required to use this library.

### How to create the JClop service

Let's suppose you have registered an application named "Test application" and *appKey* and *appSecret* are its key and secret.

```java
// Dropbox requires connection data to be initialized
final DbxAppInfo appInfo = new DbxAppInfo(appKey, appSecret);
final DbxConnectionData ctData = new DbxConnectionData("Test application", DbxRequestConfig.newBuilder("Test client").build(), appInfo);
// Create the service
final DropboxService service = new DropboxService(new File("cacheTest"), ctData);
```

### How to select a file

First you should have a Dropboxservice in the *service* variable (see above).

```java
// Create a chooser to choose a file in Dropbox
final URIChooser chooser = new DropboxURIChooser(service);
// Create a dialog that allows the selection of a file in Dropbox
final URIChooserDialog dialog = new URIChooserDialog(null, "", new URIChooser[] {chooser});
// Get the URI of a file
// The first time, you'll have to connect to your Dropbox account using a browser (this is the Dropbox way to connect to an account).
// The next time, you will not be asked again to connect (the connection data is saved in the local cache folder). 
final URI uri = dialog.showDialog();
if (uri != null) {
	// If a file was selected
	System.out.println("Full (with credentials) URI is "+uri);
	System.out.println("Displayable URI is "+service.getDisplayable(uri));
}
```

If you select a file in the dialog, this code will ouput something like
```
Full (with credentials) URI is dropbox://12345678:OAuth2-refresh-3nVCk_EhwVcAAAAAB8fAe1wcuiZ3YrBj7o4KJI482s8AKeZ9UpJO7GhZJoAB_kw@cloud.jclop.fathzer.com/userName/file
Displayable URI is dropbox://userName/file
```
**Be cautious with the full URI, it contains sensitive information**: the credentials it contains would allow anybody to obtain the same privileges your application has on the user dropbox account.

### How to read/write a file
Jclop provides a generic way for accessing files, please see [its documentation](https://github.com/jclop2/JClop).
