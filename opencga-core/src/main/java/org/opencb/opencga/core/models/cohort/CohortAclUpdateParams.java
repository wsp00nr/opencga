package org.opencb.opencga.core.models.cohort;

import org.opencb.opencga.core.models.AclParams;

public class CohortAclUpdateParams extends AclParams {

    private String cohort;

    public CohortAclUpdateParams() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CohortAclUpdateParams{");
        sb.append("cohort='").append(cohort).append('\'');
        sb.append(", permissions='").append(permissions).append('\'');
        sb.append(", action=").append(action);
        sb.append('}');
        return sb.toString();
    }

    public String getCohort() {
        return cohort;
    }

    public CohortAclUpdateParams setCohort(String cohort) {
        this.cohort = cohort;
        return this;
    }
}
