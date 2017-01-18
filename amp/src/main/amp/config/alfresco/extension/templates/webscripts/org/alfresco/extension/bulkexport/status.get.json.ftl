[#ftl]
{
[#if beStatus??]
  "processingState"                : [#if beStatus.currentStatus??]"${beStatus.currentStatus?js_string?replace("\\'", "'")?replace("\\>", ">")}"[#else]"n/a"[/#if],
  "inProgress"                     : ${beStatus.inProgress()?c},
  [#if beStatus.initiatingUserId??]
  "initiatingUserId"               : "${beStatus.initiatingUserId?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  [#if beStatus.sourceName??]
  "sourceName"                     : "${beStatus.sourceName?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  [#if beStatus.targetPath??]
  "targetPath"                     : "${beStatus.targetPath?js_string?replace("\\'", "'")?replace("\\>", ">")}",
  [/#if]
  "stopping"                       : ${beStatus.isStopping()?c},
  "paused"                         : ${beStatus.isPaused()?c},
  "succeeded"                      : ${beStatus.succeeded()?c},
  "failed"                         : ${beStatus.failed()?c},
  "stopped"                        : ${beStatus.stopped()?c},
  [#if beStatus.startDate??]
  "startDate"                      : "${beStatus.startDate?datetime?iso_utc}",
  [/#if]
  [#if beStatus.endDate??]
  "endDate"                        : "${beStatus.endDate?datetime?iso_utc}",
  [/#if]
  [#if beStatus.durationInNs??]
  "durationInNs"                   : ${beStatus.durationInNs?c},
    [#if beStatus.duration??]
  "duration"                       : "${beStatus.duration?js_string?replace("\\'", "'")?replace("\\>", ">")}",
    [/#if]
  [/#if]
  "targetCounters" : {
  [#if beStatus.targetCounterNames??]
    [#list beStatus.targetCounterNames as counterName]
      [#assign counterValue     = beStatus.getTargetCounter(counterName)!0]
    "${counterName?js_string?replace("\\'", "'")?replace("\\>", ">")}" : {
      "Count" : ${counterValue?c}
    }[#if counterName != beStatus.targetCounterNames?last],[/#if]
    [/#list]
  [/#if]
  }
[/#if]
}
