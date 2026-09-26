package com.example.util

object WebTemplates {

    val GOOGLE_APPS_SCRIPT_CODE = """
/**
 * ========================================================
 * Google Apps Script for Inventory Data Entry (Code.gs)
 * ========================================================
 * Saves entries directly to the active Google Sheet:
 * Timestamp, Item No, Item Name, Item Quantity, Item Price, Total Item Price
 */

function doGet(e) {
  return ContentService.createTextOutput(
    JSON.stringify({ 
      status: "success", 
      message: "Google Sheets Inventory Web App is active and ready." 
    })
  ).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  var lock = LockService.getScriptLock();
  // Wait up to 10 seconds for concurrent writes
  lock.tryLock(10000);

  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();

    // Auto-create and style header row if sheet is brand new
    if (sheet.getLastRow() === 0) {
      sheet.appendRow([
        "Timestamp",
        "Item No",
        "Item Name",
        "Item Quantity",
        "Item Price (Birr)",
        "Total Item Price (Birr)"
      ]);
      var headerRange = sheet.getRange(1, 1, 1, 6);
      headerRange.setFontWeight("bold");
      headerRange.setBackground("#0F9D58");
      headerRange.setFontColor("#FFFFFF");
      sheet.setFrozenRows(1);
    }

    var data;
    if (e.postData && e.postData.contents) {
      try {
        data = JSON.parse(e.postData.contents);
      } catch (err) {
        data = e.parameter;
      }
    } else {
      data = e.parameter;
    }

    var timestamp = data.timestamp || Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "yyyy-MM-dd HH:mm:ss");
    var itemNo = Number(data.itemNo) || data.itemNo || "";
    var itemName = data.itemName || "";
    var itemQuantity = Number(data.itemQuantity) || 0;
    var itemPrice = Number(data.itemPrice) || 0;
    var totalItemPrice = Number(data.totalItemPrice) || (itemQuantity * itemPrice);

    // Append new row to Google Sheet
    sheet.appendRow([
      timestamp,
      itemNo,
      itemName,
      itemQuantity,
      itemPrice,
      totalItemPrice
    ]);

    // Format currency columns (#,##0.00 "Birr")
    var newRow = sheet.getLastRow();
    sheet.getRange(newRow, 5).setNumberFormat("#,##0.00 \"Birr\"");
    sheet.getRange(newRow, 6).setNumberFormat("#,##0.00 \"Birr\"");

    return ContentService.createTextOutput(
      JSON.stringify({
        status: "success",
        message: "Item #" + itemNo + " (" + itemName + ") successfully saved to Google Sheet!",
        row: newRow
      })
    ).setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService.createTextOutput(
      JSON.stringify({ status: "error", message: error.toString() })
    ).setMimeType(ContentService.MimeType.JSON);
  } finally {
    lock.releaseLock();
  }
}
""".trimIndent()

    val HTML_BOOTSTRAP_CODE = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Inventory Data Entry Form</title>
  <!-- Bootstrap 5 CSS -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
  <!-- Bootstrap Icons -->
  <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
  <style>
    body {
      background-color: #f3f6f4;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      padding-bottom: 40px;
    }
    .header-box {
      background: linear-gradient(135deg, #0F9D58, #0B8043);
      color: white;
      border-radius: 0 0 20px 20px;
      padding: 24px 16px 28px;
      margin-bottom: 20px;
      box-shadow: 0 4px 15px rgba(15, 157, 88, 0.2);
    }
    .total-badge-card {
      background: #e8f5e9;
      border: 2px solid #a5d6a7;
      border-radius: 12px;
      padding: 16px;
      transition: all 0.2s ease;
    }
    .btn-submit {
      background-color: #0F9D58;
      border-color: #0F9D58;
      color: #ffffff;
      font-weight: 600;
      padding: 12px;
      border-radius: 10px;
      box-shadow: 0 4px 10px rgba(15, 157, 88, 0.25);
    }
    .btn-submit:hover, .btn-submit:focus {
      background-color: #0B8043;
      border-color: #0B8043;
      color: #ffffff;
    }
    .card-custom {
      border-radius: 16px;
      border: none;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    }
  </style>
</head>
<body>

  <!-- Mobile Header -->
  <div class="header-box text-center">
    <div class="d-inline-flex align-items-center justify-content-center p-2 bg-white bg-opacity-25 rounded-circle mb-2" style="width: 48px; height: 48px;">
      <i class="bi bi-table fs-4"></i>
    </div>
    <h4 class="fw-bold mb-1">Inventory Data Entry</h4>
    <p class="mb-0 text-white-50 small">Appends real-time records directly to Google Sheets</p>
  </div>

  <div class="container" style="max-width: 580px;">

    <!-- Success Confirmation Alert -->
    <div id="successAlert" class="alert alert-success alert-dismissible fade d-none align-items-center shadow-sm" role="alert">
      <div class="d-flex align-items-center">
        <i class="bi bi-check-circle-fill fs-4 me-2 text-success"></i>
        <div>
          <strong id="alertTitle">Submission Successful!</strong>
          <div id="alertMessage" class="small">The new item has been saved to your active Google Sheet.</div>
        </div>
      </div>
      <button type="button" class="btn-close" onclick="closeAlert()"></button>
    </div>

    <!-- Error Alert -->
    <div id="errorAlert" class="alert alert-danger alert-dismissible fade d-none" role="alert">
      <div class="d-flex align-items-center">
        <i class="bi bi-exclamation-triangle-fill fs-4 me-2"></i>
        <div id="errorMessage" class="small">An error occurred while submitting data.</div>
      </div>
      <button type="button" class="btn-close" onclick="document.getElementById('errorAlert').classList.add('d-none')"></button>
    </div>

    <!-- Main Entry Form Card -->
    <div class="card card-custom p-4 mb-4">
      <form id="inventoryForm" onsubmit="handleSubmit(event)">

        <!-- Google Apps Script URL Config (Collapsible) -->
        <div class="mb-3">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <label class="form-label text-muted small fw-semibold mb-0">Google Apps Script Web App URL</label>
            <a class="text-decoration-none small" data-bs-toggle="collapse" href="#urlSettings" role="button">
              <i class="bi bi-gear-fill me-1"></i>Settings
            </a>
          </div>
          <div class="collapse show" id="urlSettings">
            <input 
              type="url" 
              class="form-control form-control-sm" 
              id="scriptUrl" 
              placeholder="https://script.google.com/macros/s/.../exec"
              onchange="saveScriptUrl()"
            >
            <div class="form-text small text-muted">Paste your deployed Web App URL here.</div>
          </div>
        </div>

        <hr class="my-3 opacity-25">

        <!-- 1. Item No -->
        <div class="mb-3">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <label for="itemNo" class="form-label fw-bold mb-0">1. Item No</label>
            <div class="form-check form-switch mb-0">
              <input class="form-check-input" type="checkbox" id="autoIncrementToggle" checked onchange="toggleAutoIncrement()">
              <label class="form-check-label small text-muted" for="autoIncrementToggle">Auto-increment</label>
            </div>
          </div>
          <div class="input-group">
            <span class="input-group-text bg-light text-muted"><i class="bi bi-hash"></i></span>
            <input 
              type="number" 
              class="form-control" 
              id="itemNo" 
              value="101" 
              required
              readonly
            >
          </div>
          <div class="form-text small" id="itemNoHelp">Automatically numbers new inventory items.</div>
        </div>

        <!-- 2. Item Name -->
        <div class="mb-3">
          <label for="itemName" class="form-label fw-bold">2. Item Name</label>
          <div class="input-group">
            <span class="input-group-text bg-light text-muted"><i class="bi bi-box-seam"></i></span>
            <input 
              type="text" 
              class="form-control" 
              id="itemName" 
              placeholder="e.g. Ergonomic Office Chair, USB-C Cable..." 
              required
              autocomplete="off"
            >
          </div>
        </div>

        <div class="row g-3 mb-3">
          <!-- 3. Item Quantity -->
          <div class="col-sm-6">
            <label for="itemQuantity" class="form-label fw-bold">3. Item Quantity</label>
            <div class="input-group">
              <button class="btn btn-outline-secondary" type="button" onclick="stepQty(-1)">-</button>
              <input 
                type="number" 
                class="form-control text-center" 
                id="itemQuantity" 
                value="1" 
                min="1" 
                step="1" 
                required 
                oninput="calculateTotal()"
              >
              <button class="btn btn-outline-secondary" type="button" onclick="stepQty(1)">+</button>
            </div>
          </div>

          <!-- 4. Item Price -->
          <div class="col-sm-6">
            <label for="itemPrice" class="form-label fw-bold">4. Item Price (Birr)</label>
            <div class="input-group">
              <span class="input-group-text">Birr</span>
              <input 
                type="number" 
                class="form-control" 
                id="itemPrice" 
                placeholder="0.00" 
                min="0" 
                step="0.01" 
                required 
                oninput="calculateTotal()"
              >
            </div>
          </div>
        </div>

        <!-- Automatic Calculation: Total Item Price -->
        <div class="total-badge-card mb-4">
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <span class="badge bg-success mb-1">Live Calculation</span>
              <div class="fw-semibold text-dark">Total Item Price</div>
              <small class="text-muted" id="calcFormula">0 qty × 0.00 Birr</small>
            </div>
            <div class="text-end">
              <div class="fs-3 fw-bold text-success" id="totalPriceDisplay">0.00 Birr</div>
            </div>
          </div>
        </div>

        <!-- Submit Button -->
        <div class="d-grid gap-2">
          <button type="submit" class="btn btn-submit" id="submitBtn">
            <span id="btnSpinner" class="spinner-border spinner-border-sm d-none me-1" role="status"></span>
            <i class="bi bi-cloud-arrow-up-fill me-1" id="btnIcon"></i>
            <span id="btnText">Submit to Google Sheet</span>
          </button>
        </div>

      </form>
    </div>

    <!-- Quick Local Activity Log & CSV Export -->
    <div class="card card-custom p-3">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 class="fw-bold mb-0 text-muted small text-uppercase d-inline">Recent Submissions</h6>
          <span class="badge bg-secondary rounded-pill ms-1" id="logCount">0 entries</span>
        </div>
        <button type="button" class="btn btn-outline-success btn-sm" onclick="exportLogToCSV()" id="exportCsvBtn" disabled>
          <i class="bi bi-file-earmark-spreadsheet me-1"></i>Export CSV
        </button>
      </div>
      <div class="table-responsive">
        <table class="table table-sm table-hover align-middle mb-0" style="font-size: 0.85rem;">
          <thead class="table-light">
            <tr>
              <th>#</th>
              <th>Item</th>
              <th class="text-center">Qty</th>
              <th class="text-end">Price</th>
              <th class="text-end">Total</th>
            </tr>
          </thead>
          <tbody id="logTableBody">
            <tr id="emptyRow">
              <td colspan="5" class="text-center text-muted py-3">No submissions yet in this session.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>

  <!-- Bootstrap Bundle JS -->
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>

  <script>
    let currentItemNo = 101;
    let submissions = [];

    // Load saved script URL from localStorage
    window.addEventListener('DOMContentLoaded', () => {
      const savedUrl = localStorage.getItem('google_sheet_web_app_url');
      if (savedUrl) {
        document.getElementById('scriptUrl').value = savedUrl;
      }
      calculateTotal();
    });

    function saveScriptUrl() {
      const url = document.getElementById('scriptUrl').value.trim();
      localStorage.setItem('google_sheet_web_app_url', url);
    }

    function toggleAutoIncrement() {
      const isAuto = document.getElementById('autoIncrementToggle').checked;
      const input = document.getElementById('itemNo');
      input.readOnly = isAuto;
      if (isAuto) {
        input.value = currentItemNo;
        document.getElementById('itemNoHelp').innerText = "Automatically numbers new inventory items.";
      } else {
        document.getElementById('itemNoHelp').innerText = "Enter a custom item number.";
      }
    }

    function stepQty(delta) {
      const qtyInput = document.getElementById('itemQuantity');
      let val = parseInt(qtyInput.value) || 0;
      val = Math.max(1, val + delta);
      qtyInput.value = val;
      calculateTotal();
    }

    function calculateTotal() {
      const qty = parseFloat(document.getElementById('itemQuantity').value) || 0;
      const price = parseFloat(document.getElementById('itemPrice').value) || 0;
      const total = qty * price;

      document.getElementById('totalPriceDisplay').innerText = total.toFixed(2) + ' Birr';
      document.getElementById('calcFormula').innerText = qty + ' qty × ' + price.toFixed(2) + ' Birr';
      return total;
    }

    function closeAlert() {
      const alert = document.getElementById('successAlert');
      alert.classList.add('d-none');
      alert.classList.remove('show');
    }

    async function handleSubmit(event) {
      event.preventDefault();

      const itemNo = document.getElementById('itemNo').value;
      const itemName = document.getElementById('itemName').value.trim();
      const itemQuantity = parseInt(document.getElementById('itemQuantity').value);
      const itemPrice = parseFloat(document.getElementById('itemPrice').value);
      const totalItemPrice = calculateTotal();
      const webAppUrl = document.getElementById('scriptUrl').value.trim();

      if (!itemName) {
        alert("Please enter an Item Name.");
        return;
      }

      // Format timestamp YYYY-MM-DD HH:mm:ss
      const now = new Date();
      const timestamp = now.getFullYear() + '-' +
        String(now.getMonth() + 1).padStart(2, '0') + '-' +
        String(now.getDate()).padStart(2, '0') + ' ' +
        String(now.getHours()).padStart(2, '0') + ':' +
        String(now.getMinutes()).padStart(2, '0') + ':' +
        String(now.getSeconds()).padStart(2, '0');

      const payload = {
        timestamp: timestamp,
        itemNo: itemNo,
        itemName: itemName,
        itemQuantity: itemQuantity,
        itemPrice: itemPrice,
        totalItemPrice: totalItemPrice
      };

      // Set Loading State
      const submitBtn = document.getElementById('submitBtn');
      const btnSpinner = document.getElementById('btnSpinner');
      const btnIcon = document.getElementById('btnIcon');
      const btnText = document.getElementById('btnText');

      submitBtn.disabled = true;
      btnSpinner.classList.remove('d-none');
      btnIcon.classList.add('d-none');
      btnText.innerText = "Submitting...";

      try {
        if (webAppUrl) {
          // Google Apps Script accepts CORS POST via text/plain to avoid pre-flight issues
          const response = await fetch(webAppUrl, {
            method: 'POST',
            body: JSON.stringify(payload)
          });
        }

        // Show Success Alert
        const successAlert = document.getElementById('successAlert');
        document.getElementById('alertTitle').innerText = "Submission Successful!";
        document.getElementById('alertMessage').innerText = 
          'Item #' + itemNo + ' ("' + itemName + '") saved. Total: $' + totalItemPrice.toFixed(2);
        successAlert.classList.remove('d-none');
        successAlert.classList.add('show');

        // Add to local activity log table
        addLogEntry(itemNo, itemName, itemQuantity, itemPrice, totalItemPrice);

        // Clear Form Fields for the next entry
        document.getElementById('itemName').value = '';
        document.getElementById('itemQuantity').value = '1';
        document.getElementById('itemPrice').value = '';
        calculateTotal();

        // Increment Item No if auto-increment is enabled
        const isAuto = document.getElementById('autoIncrementToggle').checked;
        if (isAuto) {
          currentItemNo = parseInt(itemNo) + 1;
          document.getElementById('itemNo').value = currentItemNo;
        }

      } catch (err) {
        console.error("Submission error:", err);
        const errorAlert = document.getElementById('errorAlert');
        document.getElementById('errorMessage').innerText = 
          "Saved locally! (Direct fetch error: " + err.message + ". Note: If testing locally, Google Apps Script requires deployment as Web App with 'Anyone' access).";
        errorAlert.classList.remove('d-none');
        errorAlert.classList.add('show');

        // Still log entry locally
        addLogEntry(itemNo, itemName, itemQuantity, itemPrice, totalItemPrice);
      } finally {
        submitBtn.disabled = false;
        btnSpinner.classList.add('d-none');
        btnIcon.classList.remove('d-none');
        btnText.innerText = "Submit to Google Sheet";
      }
    }

    function addLogEntry(no, name, qty, price, total) {
      const tbody = document.getElementById('logTableBody');
      const emptyRow = document.getElementById('emptyRow');
      if (emptyRow) emptyRow.remove();

      const now = new Date();
      const ts = now.toISOString().slice(0, 19).replace('T', ' ');
      submissions.push({ no, name, qty, price, total, timestamp: ts });
      document.getElementById('logCount').innerText = submissions.length + ' entries';
      const exportBtn = document.getElementById('exportCsvBtn');
      if (exportBtn) exportBtn.disabled = false;

      const row = document.createElement('tr');
      row.innerHTML = 
        '<td><span class="badge bg-light text-dark border">#' + no + '</span></td>' +
        '<td class="fw-semibold">' + escapeHtml(name) + '</td>' +
        '<td class="text-center">' + qty + '</td>' +
        '<td class="text-end">' + price.toFixed(2) + ' Birr</td>' +
        '<td class="text-end text-success fw-bold">' + total.toFixed(2) + ' Birr</td>';
      tbody.prepend(row);
    }

    function exportLogToCSV() {
      if (submissions.length === 0) {
        alert("No entries to export.");
        return;
      }
      let csv = "Timestamp,Item No,Item Name,Item Quantity,Item Price (Birr),Total Item Price (Birr)\n";
      submissions.forEach(item => {
        const escaped = (item.name.includes(',') || item.name.includes('"') || item.name.includes('\n'))
          ? '"' + item.name.replace(/"/g, '""') + '"'
          : item.name;
        csv += (item.timestamp || '') + ',' + item.no + ',' + escaped + ',' + item.qty + ',' + item.price.toFixed(2) + ',' + item.total.toFixed(2) + '\n';
      });
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.setAttribute("href", url);
      link.setAttribute("download", "inventory_backup_" + new Date().toISOString().slice(0, 10) + ".csv");
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }

    function escapeHtml(text) {
      const div = document.createElement('div');
      div.innerText = text;
      return div.innerHTML;
    }
  </script>
</body>
</html>
""".trimIndent()

    val DEPLOYMENT_STEPS = listOf(
        "Step 1: Open Google Sheets" to "Create a new Google Sheet (or open an existing one). Name the sheet tab (e.g. 'Sheet1' or 'Inventory'). The script automatically configures the header row if empty!",
        "Step 2: Open Apps Script Editor" to "In the top menu of your Google Sheet, click Extensions > Apps Script. This opens the script development console.",
        "Step 3: Paste Code.gs" to "Delete any boilerplate code in Code.gs. Copy the Google Apps Script code from the tab above and paste it into the editor. Press Ctrl+S (or Cmd+S) to save.",
        "Step 4: Deploy as Web App" to "Click the blue Deploy button in the top right > New deployment.\n• Select type: Click gear icon > Web app.\n• Description: Inventory Form Webhook\n• Execute as: Me (your Google account)\n• Who has access: Anyone (CRITICAL: this allows the form/app to submit entries without complex OAuth).\nClick Deploy.",
        "Step 5: Authorize and Copy URL" to "Authorize permissions when prompted (Advanced > Go to Untitled project (unsafe) > Allow). Copy the Web app URL ending in /exec.\nPaste this URL into your form or in this app to sync live!"
    )
}
