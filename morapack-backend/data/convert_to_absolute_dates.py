#!/usr/bin/env python3
"""
Script to convert pedidos_generados.csv from relative dates (dd,hh,mm) 
to absolute timestamps (YYYY-MM-DDTHH:MM:SS)

Base date: December 1, 2025 (2025-12-01)
"""

import csv
from datetime import datetime, timedelta

# Configuration
INPUT_FILE = 'pedidos_generados.csv'
OUTPUT_FILE = 'orders.csv'
BASE_DATE = datetime(2025, 12, 1, 0, 0, 0)  # December 1, 2025, 00:00:00

print(f"🔄 Converting {INPUT_FILE} to absolute dates...")
print(f"📅 Base date: {BASE_DATE.strftime('%Y-%m-%d')}")
print()

converted_count = 0
skipped_count = 0

with open(INPUT_FILE, 'r', encoding='utf-8') as infile, \
     open(OUTPUT_FILE, 'w', newline='', encoding='utf-8') as outfile:
    
    reader = csv.DictReader(infile)
    writer = csv.DictWriter(outfile, ['timestamp', 'destination', 'quantity', 'clientId'])
    writer.writeheader()
    
    for row in reader:
        try:
            # Parse relative date components
            day = int(row['dd'])
            hour = int(row['hh'])
            minute = int(row['mm'])
            
            # Calculate absolute timestamp
            # Day 1 = Dec 1, Day 2 = Dec 2, etc.
            order_timestamp = BASE_DATE + timedelta(days=day-1, hours=hour, minutes=minute)
            
            # Write converted row
            writer.writerow({
                'timestamp': order_timestamp.strftime('%Y-%m-%dT%H:%M:%S'),
                'destination': row['dest'].strip(),
                'quantity': row['cant'].strip(),
                'clientId': row['IdClien'].strip()
            })
            
            converted_count += 1
            
            # Print progress every 100 orders
            if converted_count % 100 == 0:
                print(f"  ✓ Converted {converted_count} orders...")
                
        except Exception as e:
            print(f"  ⚠️  Skipped row due to error: {e}")
            print(f"      Row data: {row}")
            skipped_count += 1

print()
print(f"✅ Conversion complete!")
print(f"   • Converted: {converted_count} orders")
print(f"   • Skipped: {skipped_count} orders")
print(f"   • Output file: {OUTPUT_FILE}")
print()
print(f"📊 Date range: {BASE_DATE.strftime('%Y-%m-%d')} to {(BASE_DATE + timedelta(days=30)).strftime('%Y-%m-%d')}")
print(f"   (December 1-31, 2025)")

