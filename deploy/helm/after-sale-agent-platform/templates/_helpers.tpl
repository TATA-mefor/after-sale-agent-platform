{{/*
Expand the name of the chart.
*/}}
{{- define "after-sale-agent-platform.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "after-sale-agent-platform.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "after-sale-agent-platform.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "after-sale-agent-platform.labels" -}}
helm.sh/chart: {{ include "after-sale-agent-platform.chart" . }}
{{ include "after-sale-agent-platform.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "after-sale-agent-platform.selectorLabels" -}}
app.kubernetes.io/name: {{ include "after-sale-agent-platform.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "after-sale-agent-platform.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "after-sale-agent-platform.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Build SPRING_PROFILES_ACTIVE value from profile flags
*/}}
{{- define "after-sale-agent-platform.springProfiles" -}}
{{- $profiles := list -}}
{{- if .Values.profiles.prod.enabled -}}
{{- $profiles = append $profiles .Values.profiles.prod.profileName -}}
{{- end -}}
{{- if .Values.profiles.securityApiKey.enabled -}}
{{- $profiles = append $profiles .Values.profiles.securityApiKey.profileName -}}
{{- end -}}
{{- if .Values.profiles.prometheus.enabled -}}
{{- $profiles = append $profiles .Values.profiles.prometheus.profileName -}}
{{- end -}}
{{- if empty $profiles -}}
{{- $profiles = list "default" -}}
{{- end -}}
{{- join "," $profiles -}}
{{- end }}
