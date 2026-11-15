{{/* Standard labels on every object's metadata. Selectors stay on the bare `app: <name>` label the
plain manifests used: selectors are immutable, so changing them would force a delete-and-recreate. */}}
{{- define "ecommerce.labels" -}}
app.kubernetes.io/part-of: ecommerce
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}
