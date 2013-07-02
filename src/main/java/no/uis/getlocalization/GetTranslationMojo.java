package no.uis.getlocalization;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.IOUtil;

import com.getlocalization.api.GLException;
import com.getlocalization.api.GLProject;
import com.getlocalization.api.files.GLServerBusyException;
import com.getlocalization.api.files.GLTranslations;

/**
 * Fetches Getlocalization files.
 * 
 */
@Mojo(defaultPhase=LifecyclePhase.GENERATE_RESOURCES, name="get-translations")
public class GetTranslationMojo extends AbstractMojo {

  @Component
  private org.apache.maven.execution.MavenSession session;
  
  @Component
  private org.apache.maven.project.MavenProject project;
  
  @Component
  private org.apache.maven.settings.Settings settings;
  
  /**
   * File mappings from remote to local file.
   */
  @Parameter(property="gl.fileMappings", required=false)
  private Map<String, String> fileMappings;

  /**
   * Location of the file.
   */
  @Parameter(property="gl.outputDir", defaultValue="${project.build.directory}/classes", required=true)
  private File outputDirectory;

  /**
   * Getlocalization user.
   */
  @Parameter(property="gl.user", required=true)
  private String glUser;

  /**
   * Getlocalization password.
   */
  @Parameter(property="gl.password", required=true)
  private String glPassword;

  /**
   * Number of retries when the server is busy;
   */
  @Parameter(property="gl.retries", required=false, defaultValue="3")
  private int retries;

  /**
   * Getlocalization project.
   */
  @Parameter(property="gl.project", required=true)
  private String glProject;

  public void execute() throws MojoExecutionException {
    GLProject project = new GLProject(glProject, glUser, glPassword);
    GLTranslations translations = new GLTranslations(project);
    try {
      File translationZip = null;
      for (int count = retries; count > 0; count--) {
        try {
          translationZip = translations.pull();
          break;
        } catch(GLServerBusyException e) {
          if (getLog().isInfoEnabled()) {
            getLog().info("Server busy, " + ((count > 1) ? "retrying..." : ""));
          }
        }
      }
      
      if (translationZip == null) {
        throw new MojoExecutionException("Cannot fetch translations");
      }
      
      FileInputStream fis = new FileInputStream(translationZip);
      ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
      ZipEntry zipEntry;
      while ((zipEntry = zis.getNextEntry()) != null) {
        String sourceFilename = zipEntry.getName();
        String targetFilename = null;
        
        if (this.fileMappings != null) {
          targetFilename = fileMappings.get(sourceFilename);
          if (targetFilename == null) {
            getLog().warn("Could nt find mapping for " + sourceFilename + ", using original.");
          }
        }
        if (targetFilename == null) {
          targetFilename = sourceFilename;
        }
        File targetFile = new File(outputDirectory, targetFilename);
        FileOutputStream fos = new FileOutputStream(targetFile);
        IOUtil.copy(zis, fos);
        fos.close();
      }
    } catch(IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    } catch(GLException glex) {
      throw new MojoExecutionException(glex.getMessage(), glex);
    }
  }
}
