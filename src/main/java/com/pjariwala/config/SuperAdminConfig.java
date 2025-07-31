package com.pjariwala.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminConfig {

  @Value("${gca.superadmin.controls.organisations:false}")
  private boolean superAdminControlsOrganisations;

  public boolean isSuperAdminControlsOrganisations() {
    return superAdminControlsOrganisations;
  }

  public void setSuperAdminControlsOrganisations(boolean superAdminControlsOrganisations) {
    this.superAdminControlsOrganisations = superAdminControlsOrganisations;
  }
}
