const tbody = document.querySelector('#result-section tbody');

const allResultls = tbody.querySelectorAll('tr');

let tableData = [];


allResultls.forEach(row =>{
    const rowResult = row.querySelectorAll('td')
    let rowArray ={
        result: rowResult[0].textContent.trim(),
        x: parseFloat(rowResult[1].textContent.trim()),
        y: parseFloat(rowResult[2].textContent.trim()),
        r: parseFloat(rowResult[3].textContent.trim()),
        dateOfRequest: rowResult[4].textContent.trim(),
        executionTime: rowResult[5].textContent.trim()
    };

    tableData.push(rowArray)
})

console.log(tableData)