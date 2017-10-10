/**
 * 
 */
package pcs.pradeep.dubey.com.service.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import pcs.pradeep.dubey.com.project.Project;
import pcs.pradeep.dubey.com.project.ProjectList;
import pcs.pradeep.dubey.com.service.utils.ApplicationConstants;
import pcs.pradeep.dubey.com.service.utils.DataFileFolderLocations;
import pcs.pradeep.dubey.com.service.utils.Utility;

/**
 * This class do handling for all the Project related details for persisting in
 * the file system
 * 
 * @author prdubey
 *
 */
public class ProjectDao {

    enum Service {
	INSTANCE;

	/**
	 * This map will populate when all the Projects are loaded
	 */
	private HashMap<String, Project> projectMap;

	public static String UPDATE_PROJECT_ID = "999999";
	public static String CREATE_PROJECT_ID = "888888";
	public static String INTERMEDIATE_TEMP_FILE_IDENTIFIER = "TEMP_FILE";
	public static String INTERMEDIATE_PROJECT_IDENTIFIER = "PROJECT";

	/**
	 * This counter is used to track the Id of last used
	 */
	private static int lastUsedProjectId;

	private Service() {

	    projectMap = new HashMap<String, Project>();
	    try {
		loadProjectData();
	    } catch (JAXBException e) {
		e.printStackTrace();
	    }
	}

	/**
	 * Used to retrieve all the projects stored in the system
	 * 
	 * @return list of the Project Object
	 */
	private List<Project> loadProjectData() throws JAXBException {
	    List<Project> projectList = new ArrayList<Project>();
	    List<String> projectPathList = Utility
		    .listFilesForFolder(new File(DataFileFolderLocations.getProjectPath()));
	    JAXBContext jaxbContext = JAXBContext.newInstance(Project.class);
	    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

	    for (String fileLocation : projectPathList) {
		Project project = (Project) (jaxbUnmarshaller.unmarshal(new File(fileLocation)));
		if (project != null) {
		    int fetchedProjectId = Integer.parseInt(project.getId());
		    lastUsedProjectId = lastUsedProjectId < fetchedProjectId ? fetchedProjectId : lastUsedProjectId;
		    projectList.add(project);
		    projectMap.put(project.getId(), project);
		}
	    }
	    return projectList;
	}

	/**
	 * Get all the Project data
	 * 
	 * @return
	 * @throws JAXBException
	 */
	public ProjectList getAllProjects() throws JAXBException {
	    ProjectList projectList = new ProjectList();
	    Collection<Project> prjList = projectMap.values();
	    for (Project project : prjList) {
		projectList.getProjectList().add(project);
	    }
	    return projectList;
	}

	/**
	 * Used to retrieve all the project stored in the system
	 * 
	 * @return Project Value Can be Null
	 */
	public Project getProject(String projectId) throws JAXBException {
	    Project project = null;
	    project = projectMap.get(projectId);
	    if (project == null) {
		try {

		    JAXBContext jaxbContext = JAXBContext.newInstance(Project.class);
		    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		    String fileLocation = DataFileFolderLocations.getProjectPath() + projectId
			    + DataFileFolderLocations.FILE_EXTENSION;
		    project = (Project) (jaxbUnmarshaller.unmarshal(new File(fileLocation)));

		} catch (JAXBException e) {
		    throw e;
		}
	    }
	    return project;
	}

	/**
	 * This method used to create the new Project in the System Project Id
	 * generated by the system
	 * 
	 * @param projectDataXML
	 *            Stream of the Project
	 * @return Project Created by System
	 */
	public String createProject(String projectDataXML) {
	    if (lastUsedProjectId == 0) {
		lastUsedProjectId = ApplicationConstants.PROJECT_ID_OFFSET;
	    }
	    String projectId = String.valueOf(++lastUsedProjectId);
	    Project project = null;

	    try {
		HashMap intermediateProcessMap = handleProjectStream(projectDataXML, CREATE_PROJECT_ID);
		project = (Project) intermediateProcessMap.get(INTERMEDIATE_PROJECT_IDENTIFIER);
		File tempFile = (File) intermediateProcessMap.get(INTERMEDIATE_TEMP_FILE_IDENTIFIER);
		project.setId(projectId);

		String fileLocation = DataFileFolderLocations.getProjectPath() + projectId
			+ DataFileFolderLocations.FILE_EXTENSION;

		JAXBContext jaxbContext = JAXBContext.newInstance(Project.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		jaxbMarshaller.marshal(project, new FileOutputStream(fileLocation));
		tempFile.delete();

	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (JAXBException e) {
		e.printStackTrace();
	    }
	    if (project != null)
		projectMap.put(projectId, project);

	    return projectId;

	}

	/**
	 * This method update the existing Project details in the system
	 * 
	 * @param projectData
	 *            : Stream of the project Information }
	 * @return True for Success
	 */
	public boolean updateProject(String projectData) {
	    boolean isTransactionSuccess = true;
	    Project project = null;
	    try {

		HashMap intermediateProcessMap = handleProjectStream(projectData, UPDATE_PROJECT_ID);
		project = (Project) intermediateProcessMap.get(INTERMEDIATE_PROJECT_IDENTIFIER);
		File tempFile = (File) intermediateProcessMap.get(INTERMEDIATE_TEMP_FILE_IDENTIFIER);

		String fileLocation = DataFileFolderLocations.getProjectPath() + project.getId()
			+ DataFileFolderLocations.FILE_EXTENSION;

		File existingFile = new File(fileLocation);
		isTransactionSuccess = existingFile.delete();

		if (isTransactionSuccess)
		    isTransactionSuccess = tempFile.renameTo(new File(fileLocation));

	    } catch (FileNotFoundException e) {
		isTransactionSuccess = false;
		e.printStackTrace();
	    } catch (IOException e) {
		isTransactionSuccess = false;
		e.printStackTrace();
	    } catch (JAXBException e) {
		isTransactionSuccess = false;
		e.printStackTrace();
	    }
	    if (project != null)
		projectMap.put(project.getId(), project);

	    return isTransactionSuccess;
	}

	/**
	 * Delete the data of the particular customer from the system
	 * 
	 * @param projectId
	 * @return
	 */
	public boolean deleteProject(String projectId) {
	    String fileLocation = DataFileFolderLocations.getProjectPath() + projectId
		    + DataFileFolderLocations.FILE_EXTENSION;
	    File file = new File(fileLocation);
	    return file.delete();
	}

	/**
	 * @param projectData
	 * @return
	 * @throws IOException
	 * @throws JAXBException
	 */
	private HashMap handleProjectStream(String projectData, String projectId) throws IOException, JAXBException {

	    HashMap intermediateMap = new HashMap<>();
	    Project project;
	    String tempFileLocation = DataFileFolderLocations.getProjectPath() + projectId
		    + DataFileFolderLocations.FILE_EXTENSION;
	    Utility.stringToDom(projectData, tempFileLocation);

	    JAXBContext jaxbContext = JAXBContext.newInstance(Project.class);
	    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	    File tempFile = new File(tempFileLocation);
	    project = (Project) (jaxbUnmarshaller.unmarshal(tempFile));

	    intermediateMap.put(INTERMEDIATE_TEMP_FILE_IDENTIFIER, tempFile);
	    intermediateMap.put(INTERMEDIATE_PROJECT_IDENTIFIER, project);
	    return intermediateMap;
	}
    }

}
