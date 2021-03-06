package it.baeyens.cdt.refactor;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameFolderChangeHandler extends Change {
    private String myOldName;
    private String myNewName;

    public RenameFolderChangeHandler(String oldName, String newName) {
	this.myOldName = oldName;
	this.myNewName = new Path(oldName).removeLastSegments(1).append(newName).toString();
    }

    @Override
    public String getName() {
	return "Arduino Folder rename handler"; //$NON-NLS-1$
    }

    @Override
    public void initializeValidationData(IProgressMonitor pm) {
	// nothing to do

    }

    @Override
    public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
	// We are always ok
	return null;
    }

    /**
     * Here we modify all cdt projects in the workspace that have a include reference to the renamed folder/project or a child of the folder/project
     * to do so we loop through all languages of all configuration descriptions of all projects
     */
    @Override
    public Change perform(IProgressMonitor pm) throws CoreException {

	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	for (int curProject = 0; curProject < projects.length; curProject++) {
	    ICProjectDescription projectDescription = mngr.getProjectDescription(projects[curProject], true);
	    if (projectDescription != null) { // if the description is null it probably is not a cdt project
		ICConfigurationDescription[] configurationDescriptions = projectDescription.getConfigurations();
		boolean projectDescriptionChanged = false;
		for (int curConfigDescription = 0; curConfigDescription < configurationDescriptions.length; curConfigDescription++) {

		    ICFolderDescription folderDescription = configurationDescriptions[curConfigDescription].getRootFolderDescription();
		    ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();

		    // Add include path to all languages
		    for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String langId = lang.getLanguageId();
			if (langId != null && langId.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
				boolean languageChanged = false;
				ICLanguageSettingEntry[] includeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
				for (int curIncludeEntry = 0; curIncludeEntry < includeEntries.length; curIncludeEntry++) {
				    if (includeEntries[curIncludeEntry].getName().startsWith(this.myOldName)) {
					String newValue = includeEntries[curIncludeEntry].getName().replace(this.myOldName, this.myNewName);
					languageChanged = true;
					projectDescriptionChanged = true;
					includeEntries[curIncludeEntry] = new CIncludePathEntry(newValue, ICSettingEntry.VALUE_WORKSPACE_PATH);
					Activator.getDefault().getLog()
						.log(new Status(IStatus.INFO, "it.baeyens.cdt.refactor", //$NON-NLS-1$
							"changed path from " + this.myOldName + " to " //$NON-NLS-1$ //$NON-NLS-2$
								+ this.myNewName));
				    }
				}
			if (languageChanged) {
				lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includeEntries);
			}
			}
		    }
		}
		if (projectDescriptionChanged) {
		    mngr.setProjectDescription(projects[curProject], projectDescription);
		}
	    }
	}
	return null;
    }

    @Override
    public Object getModifiedElement() {
	// jaba doesn't think this is related to an element. But he may be wrong.
	return null;
    }

}
