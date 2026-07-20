package com.takat.finanzas.data.csv

import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val CSV_ENTRY = "backup.csv"
private const val ATTACHMENTS_DIR = "adjuntos/"

/** The zip container format for a full backup: backup.csv plus the (decrypted) receipt files it references. */
object BackupZip {
    fun write(output: OutputStream, csv: String, attachments: Map<String, ByteArray>) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(CSV_ENTRY))
            zip.write(csv.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            attachments.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(ATTACHMENTS_DIR + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    /**
     * Returns the CSV text plus attachment bytes keyed by filename (without the "adjuntos/" prefix).
     * Falls back to treating [bytes] as a plain CSV file with no attachments, for backups made before
     * this zip format existed.
     */
    fun read(bytes: ByteArray): Pair<String, Map<String, ByteArray>> {
        var csv: String? = null
        val files = mutableMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val content = zip.readBytes()
                    when {
                        entry.name == CSV_ENTRY -> csv = content.toString(Charsets.UTF_8)
                        entry.name.startsWith(ATTACHMENTS_DIR) -> files[entry.name.removePrefix(ATTACHMENTS_DIR)] = content
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return (csv ?: bytes.toString(Charsets.UTF_8)) to files
    }
}
