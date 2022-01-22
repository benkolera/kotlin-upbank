import com.benkolera.Account
import com.benkolera.Upbank
import com.sksamuel.hoplite.ConfigLoader
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

data class Config ( val apikey: String )

fun header(t: String) {
    println("================================================================================")
    println(t)
    println("================================================================================")
}
fun hr() {
    println("--------------------------------------------------------------------------------")
}

fun main(args: Array<String>) {
    // The easiest way to set this is in: ~/.userconfig.properties . Set that file to this:
    // apikey=up:yeah:<snip rest of personal access token>

    val config = ConfigLoader().loadConfigOrThrow<Config>()
    val up = Upbank(config.apikey)
    val acctMap = up.accounts().associateBy { it.id }
    val txns = up.transactionsSince(OffsetDateTime.now().minusDays(7))

    header("ACCOUNTS")
    acctMap.values.forEach{
        println("$${it.attributes.balance.value.padStart(10)} - ${it.attributes.displayName}")
    }
    header("TRANSACTIONS")
    txns.sortedByDescending { it.attributes.settledAt ?: it.attributes.createdAt }.forEach {
        println("${(it.attributes.settledAt ?: it.attributes.createdAt).format(DateTimeFormatter.ISO_DATE)} $${it.attributes.amount.value.padStart(10)} - ${it.attributes.description} - ${it.attributes.status}")
    }

}