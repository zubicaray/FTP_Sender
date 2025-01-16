package com.tecal;
import com.jcraft.jsch.*;
import org.ini4j.Wini;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;


public class SftpUploader {
    private static final Logger logger = LogManager.getLogger(SftpUploader.class);
    
    public static void ecrireSectionRetourDansConfig(int codeRetour, String messageRetour) throws IOException {
        String cheminConfig = "config.ini"; // Chemin vers le fichier config.ini
        File fichierConfig = new File(cheminConfig);

        // Charger ou créer le fichier INI
        Wini ini = new Wini(fichierConfig);

        // Ajouter ou mettre à jour la section [retour]
        ini.put("retour", "code_retour", codeRetour);
        ini.put("retour", "message", messageRetour);

        // Sauvegarder les modifications
        ini.store();

        System.out.println("Section [retour] mise à jour avec succès !");
    }
    
    public static void main(String[] args) {
    	int exitCode = 0; // Par défaut, succès
        String messageRetour = "Succès"; // Message de retour
        if (args.length < 1) {
            logger.error("Veuillez spécifier le chemin du fichier à envoyer en argument.");
            System.err.println("Usage: java -jar sftp-uploader.jar <chemin-fichier>");
            exitCode = 3;
            messageRetour="Usage: java -jar sftp-uploader.jar <chemin-fichier>";
          
        }

        String localFilePath = args[0]; // Fichier à envoyer
        String configFilePath = "config.ini"; // Configuration
     

        try {
            // Charger la configuration
            Wini ini = new Wini(new File(configFilePath));
            String host = ini.get("sftp", "host");
            int port = Integer.parseInt(ini.get("sftp", "port"));
            String username = ini.get("sftp", "username");
            String password = ini.get("sftp", "password");
            String remoteDir = ini.get("sftp", "remoteDir");

            logger.info("Début de la connexion SFTP à {}:{}", host, port);

            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");

            try {
                // Connexion au serveur
                session.connect();
                logger.info("Connexion établie avec succès au serveur SFTP.");

                Channel channel = session.openChannel("sftp");
                channel.connect();
                ChannelSftp sftpChannel = (ChannelSftp) channel;

                // Transférer le fichier
                logger.info("Envoi du fichier {} vers le dossier {} sur le serveur SFTP.", localFilePath, remoteDir);
                sftpChannel.cd(remoteDir);
                try (FileInputStream fis = new FileInputStream(new File(localFilePath))) {
                    sftpChannel.put(fis, new File(localFilePath).getName());
                    logger.info("Fichier envoyé avec succès !");
                    System.out.println("Succès : Le fichier a été envoyé.");
                    exitCode = 0;
                    messageRetour="Fichier envoyé avec succès !";
                } catch (SftpException e) {
                    logger.error("Erreur lors du transfert du fichier : {}", e.getMessage(), e);
                    
                    exitCode = 2;
                    messageRetour="Erreur : Échec de l'envoi du fichier.";
                } finally {
                    sftpChannel.exit();
                    logger.info("Canal SFTP fermé.");
                }
            } catch (JSchException e) {
                logger.error("Erreur lors de la connexion au serveur SFTP : {}", e.getMessage(), e);
               
                exitCode = 1;
                messageRetour="Erreur : Impossible de se connecter au serveur.";
            } finally {
                if (session.isConnected()) {
                    session.disconnect();
                    logger.info("Session SFTP déconnectée.");
                }
            }
        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier config.ini : {}", e.getMessage(), e);
           
            exitCode = 3;
            messageRetour="Erreur : Configuration invalide.";
        } catch (Exception e) {
            logger.error("Une erreur inattendue s'est produite : {}", e.getMessage(), e);
            
            exitCode = 3;
            messageRetour="Erreur : Une erreur inattendue est survenue.";
        }
        finally {
            // Écrire le code retour dans config.ini
            try {
            	logger.info("ecrireSectionRetourDansConfig !");
                ecrireSectionRetourDansConfig(exitCode, messageRetour);
            } catch (IOException e) {
                System.err.println("Impossible d'écrire dans config.ini : " + e.getMessage());
            }

            // Sortie du programme avec le code retour approprié
            System.exit(exitCode);
        }
    }
}