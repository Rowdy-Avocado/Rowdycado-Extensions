import requests

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0',
    'Accept': '*/*',
    'Accept-Language': 'en-US,en;q=0.5',
    # 'Accept-Encoding': 'gzip, deflate, br',
    'Origin': 'https://vidxstream.xyz',
    'Connection': 'keep-alive',
    'Referer': 'https://vidxstream.xyz/',
    'Sec-Fetch-Dest': 'empty',
    'Sec-Fetch-Mode': 'cors',
    'Sec-Fetch-Site': 'cross-site',
    # Requests doesn't support trailers
    'TE': 'trailers',
}

# params = {
#     'token': '8f0bfafe820163be99d3d8a2bbcc2f97',
#     'client': '6cd79b59e67dd87f4e5603f1c55c6d14',
#     'expires': '1692987633',
#     'type': 'edge',
#     'node': '3af8lvauXiCkFv93EIBIoBKlV7ut1NUVHMRPXoVD_wZ-1DxbBCFpTNkMfT8gBD-twnaHGx7aKAFXmZUcfmlG41bFsCLvEneRfcneheNPBakN8huwwoMf4m6AjOcudDzvCpE-stSOhTnaBCrXBWcgdQ3UT4HM_Coi_4moAYMO3x9S3jhf4TGKXzgnNxXOYLTA',
# }

params = {
    'token': '8f0bfafe820163be99d3d8a2bbcc2f97',
    'client': '6cd79b59e67dd87f4e5603f1c55c6d14',
    'expires': '1692987633',
    'type': 'edge',
    'node': '3af8lvauXiCkFv93EIBIoBKlV7ut1NUVHMRPXoVD_wZ-1DxbBCFpTNkMfT8gBD-twnaHGx7aKAFXmZUcfmlG41bFsCLvEneRfcneheNPBakN8huwwoMf4m6AjOcudDzvCpE-stSOhTnaBCrXBWcgdQ3UT4HM_Coi_4moAYMO3x9S3jhf4TGKXzgnNxXOYLTA',
}
response = requests.get(
    'https://hls25-eu.zcdn.stream/8bad90b6d991cc929e7fe7b70372dece/2023-07-29/360.m3u8',
    params=params,
    headers=headers,
)
print(response)
#print(response.text)

#from params
params = {
    'token': '5543b70337f7c8cca8ae3fc05be84f5c',
    'client': 'd41d8cd98f00b204e9800998ecf8427e',
    'expires': '1692989762',
    'type': 'edge',
    'node': 'myEHCCwpzGWWwNKgbchGLju5uwiphVIImYhc271Lr6n1VwA7AbtedDLDludk1VkOhHqhwKRS19HLH2c3KA2PAbxzNXtMH0s0Dwm9I8g7loSUqDP4rj1rcOlfWtICujqSVRSPSNno2yO5yRnaoJrRheB2mXv8P-PQX0-gnUnCJymkE3EY-rKQZUIaJp-Pne-z',
}

response = requests.get(
    'https://hls24-eu.zcdn.stream/4928062ec9a5902fbc4f95865a4b7a28/2023-08-18/video360.m3u8?token=5543b70337f7c8cca8ae3fc05be84f5c&client=6cd79b59e67dd87f4e5603f1c55c6d14&expires=1692987633&type=edge&node=3af8lvauXiCkFv93EIBIoBKlV7ut1NUVHMRPXoVD_wZ-1DxbBCFpTNkMfT8gBD-twnaHGx7aKAFXmZUcfmlG41bFsCLvEneRfcneheNPBakN8huwwoMf4m6AjOcudDzvCpE-stSOhTnaBCrXBWcgdQ3UT4HM_Coi_4moAYMO3x9S3jhf4TGKXzgnNxXOYLTA',
    headers=headers,
)
print(response)
print(response.text)

response = requests.get(
    'https://hls25-eu.zcdn.stream/8bad90b6d991cc929e7fe7b70372dece/2023-07-29/360.m3u8',
    params=params,
    headers=headers,
)
print(response)
print(response.text)