Thank you for submitting a contribution to Apache NiFi Registry.

Please provide a short description of the PR here:

#### Description of PR

_Enables X functionality; fixes bug NIFIREG-YYYY._

In order to streamline the review of the contribution we ask you
to ensure the following steps have been taken:

### For all changes:
- [ ] Is there a JIRA ticket associated with this PR? Is it referenced 
     in the commit message?

- [ ] Does your PR title start with **NIFIREG-XXXX** where XXXX is the JIRA number you are trying to resolve? Pay particular attention to the hyphen "-" character.

- [ ] Has your PR been rebased against the latest commit within the target branch (typically `main`)?

- [ ] Is your initial contribution a single, squashed commit? _Additional commits in response to PR reviewer feedback should be made on this branch and pushed to allow change tracking. Do not `squash` or use `--force` when pushing to allow for clean monitoring of changes._

### For code changes:
- [ ] Have you ensured that the full suite of tests is executed via `mvn -Pcontrib-check clean install` at the root `nifi-registry` folder?
- [ ] Have you written or updated unit tests to verify your changes?
- [ ] Have you verified that the full build is successful on JDK 8?
- [ ] Have you verified that the full build is successful on JDK 11?
- [ ] If adding new dependencies to the code, are these dependencies licensed in a way that is compatible for inclusion under [ASF 2.0](http://www.apache.org/legal/resolved.html#category-a)? 
- [ ] If applicable, have you updated the `LICENSE` file, including the main `LICENSE` file under `nifi-registry-assembly`?
- [ ] If applicable, have you updated the `NOTICE` file, including the main `NOTICE` file found under `nifi-registry-assembly`?

### For documentation related changes:
- [ ] Have you ensured that format looks appropriate for the output in which it is rendered?

### Note:
Please ensure that once the PR is submitted, you check GitHub Actions CI for build issues and submit an update to your PR as soon as possible.
