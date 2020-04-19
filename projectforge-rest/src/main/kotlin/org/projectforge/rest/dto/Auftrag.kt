package org.projectforge.rest.dto

import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.framework.utils.NumberFormatter
import java.math.BigDecimal
import java.time.LocalDate

class Auftrag(
        var nummer: Int? = null,
        var customer: Customer? = Customer(),
        var project: Project? = Project(),
        var titel: String? = null,
        var positionen: MutableList<AuftragsPositionDO>? = null,
        var personDays: BigDecimal? = null,
        var referenz: String? = null,
        var assignedPersons: String? = null,
        var erfassungsDatum: LocalDate? = null,
        var entscheidungsDatum: LocalDate? = null,
        var nettoSumme: BigDecimal? = null,
        var beauftragtNettoSumme: BigDecimal? = null,
        var fakturiertSum: BigDecimal? = null,
        var zuFakturierenSum: BigDecimal? = null,
        var periodOfPerformanceBegin: LocalDate? = null,
        var periodOfPerformanceEnd: LocalDate? = null,
        var probabilityOfOccurrence: Int? = null,
        var auftragsStatus: AuftragsStatus? = null
) : BaseDTO<AuftragDO>() {
    var pos: String? = null

    var formattedNettoSumme: String? = null
    var formattedBeauftragtNettoSumme: String? = null
    var formattedFakturiertSum: String? = null
    var formattedZuFakturierenSum: String? = null

    override fun copyFrom(src: AuftragDO) {
        super.copyFrom(src)

        this.customer = src.kunde?.let {
            Customer(it)
        }
        this.project = src.projekt?.let {
            Project(it)
        }

        positionen = src.positionen
        personDays = src.personDays
        assignedPersons = src.assignedPersons
        formattedNettoSumme = NumberFormatter.formatCurrency(src.nettoSumme)
        formattedBeauftragtNettoSumme = NumberFormatter.formatCurrency(src.beauftragtNettoSumme)
        formattedFakturiertSum = NumberFormatter.formatCurrency(src.fakturiertSum)
        formattedZuFakturierenSum = NumberFormatter.formatCurrency(src.zuFakturierenSum)
        pos = "#" + positionen?.size
    }
}