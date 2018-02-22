package mx.com.anzen.anzenops.control;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class APIGoogleDrive {
    /** Application name. */
    private static final String APPLICATION_NAME = "Drive API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/drive-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = APIGoogleDrive.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }

    /**
     * Metodo que solo consulta carpetas
     * @param idFolder - Identificador de la carpeta que se quiere consultar, si viene nulo se consulta la carpeta raiz
     * @return List<File>, lista de carpetas
     * @throws IOException
     */
    public static List<File> consultaCarpetas(String idFolder) throws IOException {
    	StringBuilder q = new StringBuilder();
    	q.append("mimeType = 'application/vnd.google-apps.folder' ");
    	
    	if(idFolder == null){
    		q.append("and 'root' in parents");
    	}else{
    		q.append("and '").append(idFolder).append("' in parents");
    	}
    	
    	return consultaDrive(q);
    }
    
    /**
     * Metodo que consulta los archivos de un Folder especifico
     * @param idFolder - Identificador de folder, si es nulo busca desde la carpeta raiz
     * @return List<File> con los archivos encontrados del identificador
     * @throws IOException
     */
    public static List<File> consultaArchivos(String idFolder) throws IOException {
    	StringBuilder q = new StringBuilder();
    	q.append("mimeType != 'application/vnd.google-apps.folder' ");
    	
    	if(idFolder == null){
    		q.append("and 'root' in parents");
    	}else{
    		q.append("and '").append(idFolder).append("' in parents");
    	}
    	
    	return consultaDrive(q);
    }

    
    /**
     * Metodo que consulta los archivos de un Folder especifico
     * @param idFolder - Identificador de folder, si es nulo busca desde la carpeta raiz
     * @return List<File> con los archivos encontrados del identificador
     * @throws IOException
     */
    private static List<File> consultaDrive(StringBuilder q) throws IOException {
    	// Build a new authorized API client service.
        Drive service = getDriveService();

        List<File> resultadoConsulta = null;
        String pageToken = null;
        
        do {
	        FileList result = service.files().list()
	        		.setFields("nextPageToken, files(id, name, mimeType, parents)")
	        		.setQ(q.toString())
	        		.setPageToken(pageToken)
	        		.execute();
        	
	        List<File> files = result.getFiles();
	        
	        if (files == null || files.size() == 0) {
	            System.out.println("No se encontraron resultados de la busqueda");
	            files = null;
	        } else {
	        	if(resultadoConsulta == null){
	        		resultadoConsulta = new ArrayList<>();
	        	}
	        	resultadoConsulta.addAll(files);
	        	
	            System.out.println("Archivos:");
	            for (File file : files) {
	                System.out.printf("%s (%s) (%s) \n", file.getName(), file.getId(), file.getMimeType());
	                
	                if(file.getParents() != null){
	                	
	                for(String par : file.getParents()){
	                	System.out.println("  Parent: "+par);
	                }
	                }
	            }
	        }
	        
	        pageToken = result.getNextPageToken();
    	}while (pageToken != null);
        
        return resultadoConsulta;
    }
    
    /**
     * Metodo para subir archivos
     * @param idFolder - Identificador del folder donde se almacenara el archivo
     * @throws IOException 
     */
    public static void subirArchivo(String idFolder) throws IOException{
    	Drive service = getDriveService();
    	
    	File fileMetadata = new File();
    	fileMetadata.setName("photo.jpg");
    	fileMetadata.setParents(Collections.singletonList(idFolder));
    	java.io.File filePath = new java.io.File("files/photo.jpg");
    	FileContent mediaContent = new FileContent("image/jpeg", filePath);
    	File file = service.files().create(fileMetadata, mediaContent)
    	    .setFields("id, parents")
    	    .execute();
    	System.out.println("File ID: " + file.getId());
    }
    
    /**
     * Metodo para crear una carpeta
     * @param idFolder
     * @param nombreFolder
     * @throws IOException
     */
    public static String crearCarpeta(String idFolder, String nombreFolder) throws IOException{
    	Drive service = getDriveService();
    	
    	File fileMetadata = new File();
    	fileMetadata.setName(nombreFolder);
    	fileMetadata.setMimeType("application/vnd.google-apps.folder");

    	File file = service.files().create(fileMetadata)
    	    .setFields("id")
    	    .execute();
    	System.out.println("Folder ID: " + file.getId());
    	
    	return file.getId();
    }
   
    /**
     * Metodo que descarga un archivo
     * @param idArchivo - Identificador del archivo
     * @throws IOException 
     */
    public static void descargarArchivo(String idArchivo) throws IOException{
    	Drive service = getDriveService();
    	
    	OutputStream outputStream = new ByteArrayOutputStream();
    	service.files().get(idArchivo)
    	    .executeMediaAndDownloadTo(outputStream);
    }
}