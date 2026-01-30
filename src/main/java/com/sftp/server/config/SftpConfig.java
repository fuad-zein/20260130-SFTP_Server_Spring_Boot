package com.sftp.server.config;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class SftpConfig {

  @Value("${sftp.server.port:2222}")
  private int sftpPort;

  private String userName = null;
  private Path userFolder = null;

  private final Map<String, String> userPasswords = Map.of(
      "user1", "pass123",
      "user2", "pass456");

  private final Map<String, Path> userFolders = Map.of(
      "user1", Paths.get("/home/fuzi/Documents/Experiments/StoreFolders/user1").toAbsolutePath(),
      "user2", Paths.get("/home/fuzi/Documents/Experiments/StoreFolders/user2").toAbsolutePath());

  @PostConstruct
  public void startSftpServer() throws IOException {
    SshServer sshd = SshServer.setUpDefaultServer();
    sshd.setPort(sftpPort); // * port sftp

    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser")));
    sshd.setPasswordAuthenticator(passwordAuthenticator());

    // ? setup default folder for user
    sshd.setFileSystemFactory(new VirtualFileSystemFactory() {

      @Override
      public FileSystem createFileSystem(SessionContext session) throws IOException {
        String username = session.getUsername();
        Path userHome = userFolders.getOrDefault(username, userFolder.toAbsolutePath());
        this.setUserHomeDir(username, userHome);
        return super.createFileSystem(session);
      }
    });

    // ? activate subsystem sftp
    sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
    sshd.start();
    System.out.println("✅ SFTP Server started on port 2222, root: " + userFolder);
  }

  private PasswordAuthenticator passwordAuthenticator() {
    return new PasswordAuthenticator() {

      @Override
      public boolean authenticate(String username, String password, ServerSession session)
          throws PasswordChangeRequiredException, AsyncAuthException {
        userName = username;
        boolean isUserValid = userPasswords.containsKey(username);
        boolean isPasswordValid = userPasswords.get(username).equals(password);

        if (isUserValid) {
          userFolder = userFolders.get(username);
          System.out.println("User is valid : " + username);
          System.out.println("User folder is : " + userFolder);
        } else {
          System.out.println("User is not valid : " + username);
        }

        return isUserValid && isPasswordValid;
      }

    };
  }

}
