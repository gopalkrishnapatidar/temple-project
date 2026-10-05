{{/*
Chart label value (helm.sh/chart).
*/}}
{{- define "temple.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Stable backend selector labels (must not include Release.Name or chart version).
*/}}
{{- define "temple.backend.selectorLabels" -}}
app.kubernetes.io/name: temple-platform
app.kubernetes.io/component: backend
{{- end }}

{{/*
Stable frontend selector labels.
*/}}
{{- define "temple.frontend.selectorLabels" -}}
app.kubernetes.io/name: temple-platform
app.kubernetes.io/component: frontend
{{- end }}

{{/*
Backend resource metadata labels.
*/}}
{{- define "temple.backend.labels" -}}
{{ include "temple.backend.selectorLabels" . }}
app.kubernetes.io/part-of: temple-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "temple.chart" . }}
{{- end }}

{{/*
Frontend resource metadata labels.
*/}}
{{- define "temple.frontend.labels" -}}
{{ include "temple.frontend.selectorLabels" . }}
app.kubernetes.io/part-of: temple-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "temple.chart" . }}
{{- end }}

{{/*
Ingress resource metadata labels.
*/}}
{{- define "temple.ingress.labels" -}}
app.kubernetes.io/name: temple-platform
app.kubernetes.io/component: ingress
app.kubernetes.io/part-of: temple-platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ include "temple.chart" . }}
{{- end }}

{{/*
Pod template labels for backend workloads.
*/}}
{{- define "temple.backend.podLabels" -}}
{{ include "temple.backend.selectorLabels" . }}
app.kubernetes.io/part-of: temple-platform
{{- end }}

{{/*
Pod template labels for frontend workloads.
*/}}
{{- define "temple.frontend.podLabels" -}}
{{ include "temple.frontend.selectorLabels" . }}
app.kubernetes.io/part-of: temple-platform
{{- end }}

{{/*
Backend container image reference.
*/}}
{{- define "temple.backend.image" -}}
{{- printf "%s:%s" .Values.backend.image.repository .Values.backend.image.tag }}
{{- end }}

{{/*
Frontend container image reference.
*/}}
{{- define "temple.frontend.image" -}}
{{- printf "%s:%s" .Values.frontend.image.repository .Values.frontend.image.tag }}
{{- end }}
