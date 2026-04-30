# CI/CD bootstrap — OpenShift Pipelines + GitOps

Ansible playbook that provisions everything needed to build and deploy
`movies-quarkus` on an OpenShift cluster with the **OpenShift Pipelines** and
**OpenShift GitOps** operators already installed.

---

## GitFlow and promotion strategy

### Branch model

```
develop ──────────────────────────────────────────► (feature integration)
   │  PR merge
   ▼
release ──────────────────────────────────────────► (stabilisation / RC)
   │  PR merge
   ▼
main ─────────────────────────────────────────────► (production)
```

| Branch    | Purpose                                | Trigger action          |
|-----------|----------------------------------------|-------------------------|
| `develop` | Integration of finished features       | Build + deploy to **dev** |
| `release` | Stabilisation, hotfixes, RC testing    | Promote to **hml**      |
| `main`    | Current production state               | Promote to **prd**      |

### Build-once, promote-many

The cardinal rule: **the container image is built exactly once** (on `develop`)
and the same artifact is promoted through all environments without rebuilding.

```
Push to develop
  └─ CI pipeline ──► mvn test ──► buildah ──► quay.io/…:abc1234
                                               │
                                       update values-dev.yaml
                                               │
                                       ArgoCD syncs → movies-quarkus-dev

PR develop → release, merge
  └─ Promote pipeline ──► read tag (abc1234) from values-dev.yaml
                        ──► write abc1234 to values-hml.yaml
                               │
                       ArgoCD syncs → movies-quarkus-hml

PR release → main, merge
  └─ Promote pipeline ──► read tag (abc1234) from values-hml.yaml
                        ──► write abc1234 to values-prd.yaml
                               │
                       ArgoCD syncs → movies-quarkus-prd
```

**Why not rebuild on every branch?** Because rebuilding produces a different
binary (different timestamp, different layer digest). You would be testing a
different artifact in hml/prd than the one validated in dev — defeating the
purpose of the test environment. "Build once" is the foundation of a
trustworthy delivery pipeline.

### Typical developer workflow

```
# 1. Feature work
git checkout develop
git checkout -b feature/my-feature
# ... code ...
git push origin feature/my-feature
# open PR → develop, get review, merge

# 2. Promote dev → hml
git checkout release
git merge develop
git push origin release     ← triggers promotion pipeline

# 3. Promote hml → prd
git checkout main
git merge release
git push origin main        ← triggers promotion pipeline
```

Hotfixes for production follow the same path: branch from `main`, merge back
to `main` and also back to `release` and `develop`.

---

## Architecture overview

```
GitHub webhook ──► EventListener (Tekton)
                    ├── develop push ──► movies-quarkus-ci Pipeline
                    │                   git-clone → maven-test → buildah
                    │                   → update values-dev.yaml
                    │
                    ├── release push ──► movies-quarkus-promote Pipeline
                    │                   values-dev.yaml → values-hml.yaml
                    │
                    └── main push ────► movies-quarkus-promote Pipeline
                                        values-hml.yaml → values-prd.yaml

GitOps repo commit ──► ArgoCD
                         ├── movies-quarkus-dev  → namespace movies-quarkus-dev
                         ├── movies-quarkus-hml  → namespace movies-quarkus-hml
                         └── movies-quarkus-prd  → namespace movies-quarkus-prd
```

---

## Namespaces

| Namespace            | Purpose                            |
|----------------------|------------------------------------|
| `movies-quarkus-ci`  | Tekton pipelines, tasks, triggers  |
| `movies-quarkus-dev` | Dev application (auto-synced)      |
| `movies-quarkus-hml` | Homolog application (auto-synced)  |
| `movies-quarkus-prd` | Production application (auto-synced)|

---

## Layout

```
ansible/
├── inventory.yml
├── playbook.yml
├── vars/main.yml
└── manifests/
    ├── base/          # namespaces (ci + dev/hml/prd), RBAC, PVCs
    ├── tasks/         # git-clone, maven-test, buildah, update-gitops, promote-gitops
    ├── pipeline/
    │   ├── pipeline-ci.yaml.j2       # build pipeline (develop branch)
    │   ├── pipeline-promote.yaml.j2  # promotion pipeline (release/main)
    │   └── pipelinerun-sample.yaml.j2
    ├── triggers/      # EventListener (3 triggers), TriggerTemplates, Bindings, Route
    └── gitops/        # 3 ArgoCD Applications (dev, hml, prd)
```

All `*.yaml.j2` files are Jinja2 templates rendered by the playbook into
`./_rendered/` and then applied with `kubernetes.core.k8s`.

---

## Prerequisites

- `oc` logged in to the cluster with enough permissions to create namespaces
  and ClusterRoleBindings.
- Operators on the cluster: **OpenShift Pipelines** and **OpenShift GitOps**.
- Local tooling:
  ```sh
  pip install ansible kubernetes openshift pyyaml
  ansible-galaxy collection install kubernetes.core
  ```

---

## Configure

Edit [vars/main.yml](vars/main.yml). The defaults match this project.
Notable variables:

| Variable               | Default                                        |
|------------------------|------------------------------------------------|
| `ci_namespace`         | `movies-quarkus-ci`                            |
| `namespace_dev/hml/prd`| `movies-quarkus-dev/hml/prd`                   |
| `image_repository`     | `quay.io/rh-ee-bcordeir/movies-quarkus`        |
| `gitops_repo_url`      | `https://github.com/rh-bcordeir/movies-quarkus-devops` |
| `webhook_secret_token` | **change before running**                      |

---

## Run

```sh
cd ansible
ansible-playbook -i inventory.yml playbook.yml
```

Skip the ArgoCD Applications (manage them separately):
```sh
ansible-playbook -i inventory.yml playbook.yml --skip-tags gitops
```

Render manifests only (no apply):
```sh
ansible-playbook -i inventory.yml playbook.yml -e apply=false
```

---

## Post-install

### 1. Create the Quay.io push secret

The `buildah` task pushes to `quay.io/rh-ee-bcordeir/movies-quarkus`.
Create a robot account on Quay with write access, then:

```sh
oc create secret docker-registry quay-push-secret \
  --docker-server=quay.io \
  --docker-username=<robot-or-username> \
  --docker-password=<token> \
  -n movies-quarkus-ci

oc secrets link pipeline-sa quay-push-secret --for=mount -n movies-quarkus-ci
```

### 2. Create the GitOps push credentials secret

The `update-gitops` and `promote-gitops` tasks push commits to the GitOps repo.
Generate a GitHub PAT with `repo` scope:

```sh
USER=<your-github-username>
TOKEN=<your-github-pat>
oc create secret generic gitops-git-credentials \
  --from-literal=.git-credentials="https://${USER}:${TOKEN}@github.com" \
  --from-literal=.gitconfig=$'[credential]\n  helper = store' \
  -n movies-quarkus-ci
```

### 3. Configure the GitHub webhook

```sh
oc get route el-movies-quarkus-listener -n movies-quarkus-ci \
  -o jsonpath='{.spec.host}'
```

In the `movies-quarkus` GitHub repo: **Settings → Webhooks → Add webhook**
- Payload URL: `https://<route-host>`
- Content type: `application/json`
- Secret: value of `webhook_secret_token` in `vars/main.yml`
- Events: *Just the push event*

**One webhook covers all three branches** — the EventListener routes each
push to the correct pipeline via CEL branch filters.

### 4. (Optional) Trigger a CI build manually

```sh
oc create -f _rendered/pipeline/pipelinerun-sample.yaml
tkn pipelinerun logs -L -f -n movies-quarkus-ci
```

---

## What gets created

| Resource | Namespace |
|----------|-----------|
| Namespaces: `movies-quarkus-ci/dev/hml/prd` | — |
| `ServiceAccount pipeline-sa` | `movies-quarkus-ci` |
| `RoleBinding pipeline-sa-edit` | `movies-quarkus-ci` |
| `PVC pipeline-workspace, maven-cache` | `movies-quarkus-ci` |
| `Task git-clone, maven-test, buildah` | `movies-quarkus-ci` |
| `Task update-gitops, promote-gitops` | `movies-quarkus-ci` |
| `Pipeline movies-quarkus-ci` | `movies-quarkus-ci` |
| `Pipeline movies-quarkus-promote` | `movies-quarkus-ci` |
| `EventListener movies-quarkus-listener` (3 triggers) | `movies-quarkus-ci` |
| `Route el-movies-quarkus-listener` (TLS edge) | `movies-quarkus-ci` |
| `Application movies-quarkus-dev/hml/prd` | `openshift-gitops` |

---

## Cleanup

```sh
oc delete application movies-quarkus-dev movies-quarkus-hml movies-quarkus-prd \
  -n openshift-gitops
oc delete namespace movies-quarkus-ci movies-quarkus-dev movies-quarkus-hml movies-quarkus-prd
```
